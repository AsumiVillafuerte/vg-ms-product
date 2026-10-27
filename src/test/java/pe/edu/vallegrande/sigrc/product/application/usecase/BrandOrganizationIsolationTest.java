package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.exception.UnauthorizedException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aislamiento multi-tenant de marcas entre dos organizaciones y dos usuarios.
 *
 * <p>El organizationId se toma siempre del token (proveedor), nunca del cuerpo de la
 * peticion, y todas las consultas se filtran por ese valor. Un usuario de la
 * organizacion B no puede leer/modificar/borrar marcas de la organizacion A.</p>
 */
@ExtendWith(MockitoExtension.class)
class BrandOrganizationIsolationTest {

    private static final String ORG_A = "org-A";
    private static final String ORG_B = "org-B";

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    private CreateBrandUseCaseImpl createBrandUseCase;
    private GetBrandUseCaseImpl getBrandUseCase;
    private UpdateBrandUseCaseImpl updateBrandUseCase;
    private DeleteBrandUseCaseImpl deleteBrandUseCase;
    private RestoreBrandUseCaseImpl restoreBrandUseCase;

    @BeforeEach
    void setUp() {
        createBrandUseCase = new CreateBrandUseCaseImpl(brandRepository, brandMapper, currentUserProvider);
        getBrandUseCase = new GetBrandUseCaseImpl(brandRepository, brandMapper, currentUserProvider);
        updateBrandUseCase = new UpdateBrandUseCaseImpl(brandRepository, brandMapper, currentUserProvider);
        deleteBrandUseCase = new DeleteBrandUseCaseImpl(brandRepository, currentUserProvider);
        restoreBrandUseCase = new RestoreBrandUseCaseImpl(brandRepository, currentUserProvider);
    }

    @Test
    void createStampsOrganizationFromToken() {
        UUID brandId = UUID.randomUUID();
        CreateBrandRequest request = new CreateBrandRequest("Samsung");
        Brand domain = Brand.builder().id(brandId).name("Samsung").status('A').build();
        BrandResponse response = BrandResponse.builder().id(brandId).name("Samsung").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_A));
        when(brandMapper.toDomain(request)).thenReturn(domain);
        when(brandRepository.existsByName("Samsung", ORG_A)).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(response);

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Brand> savedCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(savedCaptor.capture());
        assertEquals(ORG_A, savedCaptor.getValue().getOrganizationId());
        verify(brandRepository).existsByName("Samsung", ORG_A);
    }

    @Test
    void createOverwritesAnyOrganizationOnMappedDomain() {
        UUID brandId = UUID.randomUUID();
        CreateBrandRequest request = new CreateBrandRequest("LG");
        Brand forged = Brand.builder().id(brandId).organizationId(ORG_B).name("LG").status('A').build();
        BrandResponse response = BrandResponse.builder().id(brandId).name("LG").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_A));
        when(brandMapper.toDomain(request)).thenReturn(forged);
        when(brandRepository.existsByName("LG", ORG_A)).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(response);

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Brand> savedCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(savedCaptor.capture());
        assertEquals(ORG_A, savedCaptor.getValue().getOrganizationId());
        verify(brandRepository, never()).existsByName(anyString(), eq(ORG_B));
    }

    @Test
    void userFromOrgBCannotGetBrandFromOrgA() {
        UUID brandId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(brandRepository.findById(brandId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(getBrandUseCase.getBrandById(brandId))
                .expectError(NotFoundException.class)
                .verify();

        verify(brandRepository).findById(brandId, ORG_B);
        verify(brandRepository, never()).findById(brandId, ORG_A);
        verify(brandMapper, never()).toResponse(any(Brand.class));
    }

    @Test
    void userFromOrgBCannotUpdateBrandFromOrgA() {
        UUID brandId = UUID.randomUUID();
        UpdateBrandRequest request = new UpdateBrandRequest("Hack");
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(brandRepository.findById(brandId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .expectError(NotFoundException.class)
                .verify();

        verify(brandRepository, never()).update(any(Brand.class));
    }

    @Test
    void userFromOrgBCannotDeleteBrandFromOrgA() {
        UUID brandId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(brandRepository.findById(brandId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(deleteBrandUseCase.logicalDeleteBrand(brandId))
                .expectError(NotFoundException.class)
                .verify();

        verify(brandRepository, never()).update(any(Brand.class));
    }

    @Test
    void userFromOrgBCannotRestoreBrandFromOrgA() {
        UUID brandId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(brandRepository.findById(brandId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(restoreBrandUseCase.restoreBrand(brandId))
                .expectError(NotFoundException.class)
                .verify();

        verify(brandRepository, never()).update(any(Brand.class));
    }

    @Test
    void getAllBrandsOnlyQueriesCallerOrganization() {
        UUID brandId = UUID.randomUUID();
        Brand brandB = Brand.builder().id(brandId).organizationId(ORG_B).name("Samsung").status('A').build();
        BrandResponse response = BrandResponse.builder().id(brandId).organizationId(ORG_B).name("Samsung").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(brandRepository.findAll(ORG_B)).thenReturn(Flux.just(brandB));
        when(brandMapper.toResponse(brandB)).thenReturn(response);

        StepVerifier.create(getBrandUseCase.getAllBrands())
                .expectNext(response)
                .verifyComplete();

        verify(brandRepository).findAll(ORG_B);
        verify(brandRepository, never()).findAll(ORG_A);
    }

    @Test
    void sameNameAllowedInDifferentOrganizations() {
        UUID brandA = UUID.randomUUID();
        UUID brandB = UUID.randomUUID();
        CreateBrandRequest request = new CreateBrandRequest("Samsung");
        Brand domainA = Brand.builder().id(brandA).name("Samsung").status('A').build();
        Brand domainB = Brand.builder().id(brandB).name("Samsung").status('A').build();
        BrandResponse responseA = BrandResponse.builder().id(brandA).name("Samsung").build();
        BrandResponse responseB = BrandResponse.builder().id(brandB).name("Samsung").build();

        when(currentUserProvider.getOrganizationId())
                .thenReturn(Mono.just(ORG_A))
                .thenReturn(Mono.just(ORG_B));
        when(brandMapper.toDomain(request)).thenReturn(domainA, domainB);
        when(brandRepository.existsByName("Samsung", ORG_A)).thenReturn(Mono.just(false));
        when(brandRepository.existsByName("Samsung", ORG_B)).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(responseA, responseB);

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectNext(responseA)
                .verifyComplete();
        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectNext(responseB)
                .verifyComplete();

        verify(brandRepository).existsByName("Samsung", ORG_A);
        verify(brandRepository).existsByName("Samsung", ORG_B);
    }

    @Test
    void missingOrganizationFailsClosedWithoutQueryingRepository() {
        UUID brandId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId())
                .thenReturn(Mono.error(new UnauthorizedException("sin organizacion")));

        StepVerifier.create(getBrandUseCase.getBrandById(brandId))
                .expectError(UnauthorizedException.class)
                .verify();

        verify(brandRepository, never()).findById(eq(brandId), anyString());
        verify(brandRepository, never()).findAll(anyString());
        verify(brandRepository, never()).save(any(Brand.class));
        verify(brandRepository, never()).existsByName(anyString(), anyString());
    }
}