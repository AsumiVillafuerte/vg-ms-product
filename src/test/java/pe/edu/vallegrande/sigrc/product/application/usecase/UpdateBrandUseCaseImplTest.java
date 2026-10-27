package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.ConflictException;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link UpdateBrandUseCaseImpl}.
 * Valida la actualizacion de marcas, incluyendo actualizacion
 * completa, parcial, duplicado, nombre propio y marca inexistente.
 */
@ExtendWith(MockitoExtension.class)
class UpdateBrandUseCaseImplTest {

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private UpdateBrandUseCaseImpl updateBrandUseCase;

    private UUID brandId;
    private Brand existingBrand;
    private BrandResponse brandResponse;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
        existingBrand = Brand.builder()
                .id(brandId).organizationId("org-1").name("Samsung").status('A').build();
        brandResponse = BrandResponse.builder()
                .id(brandId).name("Samsung Electronics").status('A').build();
    }

    /**
     * Caso positivo: actualiza el nombre de una marca existente.
     * Verifica que se verifique el duplicado y se retorne la respuesta actualizada.
     */
    @Test
    void shouldUpdateBrandSuccessfully() {
        UpdateBrandRequest request = new UpdateBrandRequest("Samsung Electronics");
        Brand mappedBrand = Brand.builder().name("Samsung Electronics").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(existingBrand));
        when(brandMapper.mapUpdateToDomain(request)).thenReturn(mappedBrand);
        when(brandRepository.existsByName("Samsung Electronics", "org-1", brandId))
                .thenReturn(Mono.just(false));
        when(brandRepository.update(any(Brand.class))).thenReturn(Mono.just(mappedBrand));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(brandResponse);

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("Samsung Electronics", response.getName());
                })
                .verifyComplete();

        verify(brandRepository).existsByName("Samsung Electronics", "org-1", brandId);
    }

    /**
     * Caso de duplicado: renombrar a un nombre existente en la misma organizacion
     * debe lanzar {@link ConflictException} sin actualizar.
     */
    @Test
    void shouldReturnConflictWhenNewNameExistsInSameOrganization() {
        UpdateBrandRequest request = new UpdateBrandRequest("Sony");
        Brand mappedBrand = Brand.builder().name("Sony").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(existingBrand));
        when(brandMapper.mapUpdateToDomain(request)).thenReturn(mappedBrand);
        when(brandRepository.existsByName("Sony", "org-1", brandId)).thenReturn(Mono.just(true));

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .expectError(ConflictException.class)
                .verify();

        verify(brandRepository, never()).update(any(Brand.class));
    }

    /**
     * Caso especial: conservar el propio nombre (cambiando solo mayusculas)
     * no debe considerarse duplicado y no requiere consulta de existencia.
     */
    @Test
    void shouldAllowKeepingOwnNameCaseInsensitively() {
        UpdateBrandRequest request = new UpdateBrandRequest("samsung");
        Brand mappedBrand = Brand.builder().name("samsung").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(existingBrand));
        when(brandMapper.mapUpdateToDomain(request)).thenReturn(mappedBrand);
        when(brandRepository.update(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(brandResponse);

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

        verify(brandRepository, never()).existsByName(anyString(), anyString(), any(UUID.class));
    }

    /**
     * Caso negativo: el request tiene campos nulos, por lo que se
     * conservan los valores originales de la marca (sin chequeo de duplicado).
     */
    @Test
    void shouldKeepExistingValuesWhenFieldsAreNull() {
        UpdateBrandRequest request = new UpdateBrandRequest(null);
        Brand mappedBrand = Brand.builder().build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(existingBrand));
        when(brandMapper.mapUpdateToDomain(request)).thenReturn(mappedBrand);
        when(brandRepository.update(any(Brand.class))).thenReturn(Mono.just(existingBrand));
        when(brandMapper.toResponse(any(Brand.class))).thenReturn(brandResponse);

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

        verify(brandRepository).update(argThat(brand -> brand.getName().equals("Samsung")));
        verify(brandRepository, never()).existsByName(anyString(), anyString(), any(UUID.class));
    }

    /**
     * Caso de carrera: el chequeo previo pasa pero la base de datos rechaza por
     * restriccion UNIQUE ({@link DuplicateKeyException}). El error debe propagarse
     * (GlobalExceptionHandler lo traduce a 409).
     */
    @Test
    void shouldPropagateDuplicateKeyExceptionOnConcurrentUpdate() {
        UpdateBrandRequest request = new UpdateBrandRequest("Sony");
        Brand mappedBrand = Brand.builder().name("Sony").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(existingBrand));
        when(brandMapper.mapUpdateToDomain(request)).thenReturn(mappedBrand);
        when(brandRepository.existsByName("Sony", "org-1", brandId)).thenReturn(Mono.just(false));
        when(brandRepository.update(any(Brand.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("duplicate key value violates unique constraint")));

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    /**
     * Caso de excepcion: se intenta actualizar una marca que no existe.
     * Verifica que se lance NotFoundException antes de consultar duplicados.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenBrandDoesNotExist() {
        UpdateBrandRequest request = new UpdateBrandRequest("Nuevo");

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(updateBrandUseCase.updateBrand(brandId, request))
                .expectError(NotFoundException.class)
                .verify();

        verify(brandRepository, never()).existsByName(anyString(), anyString(), any(UUID.class));
        verify(brandRepository, never()).update(any(Brand.class));
    }
}