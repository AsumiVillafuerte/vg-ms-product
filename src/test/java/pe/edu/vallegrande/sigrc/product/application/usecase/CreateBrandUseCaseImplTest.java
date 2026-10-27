package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.ConflictException;
import pe.edu.vallegrande.sigrc.product.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link CreateBrandUseCaseImpl}.
 * Valida la creacion de marcas cubriendo casos positivos,
 * negativos, de duplicado y de excepcion.
 */
@ExtendWith(MockitoExtension.class)
class CreateBrandUseCaseImplTest {

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private CreateBrandUseCaseImpl createBrandUseCase;

    private CreateBrandRequest request;
    private Brand brandDomain;
    private BrandResponse brandResponse;
    private UUID brandId;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
        request = new CreateBrandRequest("Samsung");
        brandDomain = Brand.builder()
                .id(brandId).name("Samsung").status('A').build();
        brandResponse = BrandResponse.builder()
                .id(brandId).name("Samsung").status('A').build();
    }

    /**
     * Caso positivo: crea una marca exitosamente con nombre valido y sin duplicados.
     * Verifica que el repositorio reciba el dominio y retorne la respuesta correcta.
     */
    @Test
    void shouldCreateBrandSuccessfully() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(request)).thenReturn(brandDomain);
        when(brandRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class))).thenReturn(Mono.just(brandDomain));
        when(brandMapper.toResponse(brandDomain)).thenReturn(brandResponse);

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(brandId, response.getId());
                    assertEquals("Samsung", response.getName());
                    assertEquals('A', response.getStatus());
                })
                .verifyComplete();

        verify(brandRepository).existsByName("Samsung", "org-1");
    }

    /**
     * Caso de duplicado: el nombre ya existe en la misma organizacion.
     * Verifica que se lance {@link ConflictException} y no se guarde.
     */
    @Test
    void shouldReturnConflictWhenNameAlreadyExistsInSameOrganization() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(request)).thenReturn(brandDomain);
        when(brandRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(true));

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectError(ConflictException.class)
                .verify();

        verify(brandRepository, never()).save(any(Brand.class));
    }

    /**
     * Caso de normalizacion: se eliminan espacios exteriores del nombre
     * antes de verificar duplicados y guardar.
     */
    @Test
    void shouldTrimNameBeforeDuplicateCheckAndSave() {
        CreateBrandRequest untrimmed = new CreateBrandRequest("  Samsung  ");
        Brand untrimmedDomain = Brand.builder()
                .id(brandId).name("  Samsung  ").status('A').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(untrimmed)).thenReturn(untrimmedDomain);
        when(brandRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(brandResponse);

        StepVerifier.create(createBrandUseCase.createBrand(untrimmed))
                .expectNext(brandResponse)
                .verifyComplete();

        verify(brandRepository).existsByName("Samsung", "org-1");
        ArgumentCaptor<Brand> savedCaptor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(savedCaptor.capture());
        assertEquals("Samsung", savedCaptor.getValue().getName());
    }

    /**
     * Caso negativo: condicion de carrera. El chequeo previo pasa (no hay duplicado)
     * pero el repositorio rechaza por restriccion UNIQUE en base de datos.
     * Verifica que el error del repositorio se propague.
     */
    @Test
    void shouldReturnErrorWhenNameIsDuplicate() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(request)).thenReturn(brandDomain);
        when(brandRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class)))
                .thenReturn(Mono.error(new RuntimeException("duplicate key value violates unique constraint")));

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectError(RuntimeException.class)
                .verify();
    }

    /**
     * Caso de excepcion: el repositorio falla por error de conexion
     * o indisponibilidad de la base de datos.
     * Verifica que la excepcion se propague sin ser capturada.
     */
    @Test
    void shouldReturnErrorWhenRepositoryFails() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(request)).thenReturn(brandDomain);
        when(brandRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(false));
        when(brandRepository.save(any(Brand.class)))
                .thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectError(RuntimeException.class)
                .verify();
    }

    /**
     * Caso de validacion: el nombre de la marca no puede estar en blanco.
     * Verifica que se lance {@link DomainException} antes de tocar el repositorio.
     */
    @Test
    void shouldRejectBlankName() {
        CreateBrandRequest blankRequest = new CreateBrandRequest("   ");
        Brand blankDomain = Brand.builder().id(brandId).name("   ").status('A').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandMapper.toDomain(blankRequest)).thenReturn(blankDomain);

        StepVerifier.create(createBrandUseCase.createBrand(blankRequest))
                .expectError(DomainException.class)
                .verify();

        verify(brandRepository, never()).existsByName(anyString(), anyString());
        verify(brandRepository, never()).save(any(Brand.class));
    }

    /**
     * Caso de organizacion: la verificacion de duplicados usa la organizacion del token
     * (el mismo nombre en otra organizacion no debe considerarse duplicado).
     */
    @Test
    void shouldCheckDuplicatesAgainstTokenOrganization() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-custom"));
        when(brandMapper.toDomain(request)).thenReturn(brandDomain);
        when(brandRepository.existsByName("Samsung", "org-custom")).thenReturn(Mono.just(true));

        StepVerifier.create(createBrandUseCase.createBrand(request))
                .expectError(ConflictException.class)
                .verify();

        verify(brandRepository, never()).existsByName(eq("Samsung"), eq("org-1"));
    }
}