package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link RestoreBrandUseCaseImpl}.
 * Valida la restauracion de marcas inactivas, el caso de una
 * marca ya activa y el manejo de marca inexistente.
 */
@ExtendWith(MockitoExtension.class)
class RestoreBrandUseCaseImplTest {

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private RestoreBrandUseCaseImpl restoreBrandUseCase;

    private UUID brandId;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
    }

    /**
     * Caso positivo: restaura una marca inactiva cambiando su estado a 'A'.
     * Verifica que el repositorio reciba la actualizacion con estado activo.
     */
    @Test
    void shouldRestoreBrandSuccessfully() {
        Brand inactiveBrand = Brand.builder()
                .id(brandId).name("Samsung").status('I').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(inactiveBrand));
        when(brandRepository.update(any(Brand.class))).thenReturn(Mono.just(inactiveBrand));

        StepVerifier.create(restoreBrandUseCase.restoreBrand(brandId))
                .verifyComplete();

        verify(brandRepository).update(argThat(b -> b.getStatus().equals('A')));
    }

    /**
     * Caso negativo: se intenta restaurar una marca que ya esta activa.
     * Verifica que la operacion se complete sin error, manteniendo el estado 'A'.
     */
    @Test
    void shouldHandleRestoreOfAlreadyActiveBrand() {
        Brand activeBrand = Brand.builder()
                .id(brandId).name("Samsung").status('A').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(activeBrand));
        when(brandRepository.update(any(Brand.class))).thenReturn(Mono.just(activeBrand));

        StepVerifier.create(restoreBrandUseCase.restoreBrand(brandId))
                .verifyComplete();
    }

    /**
     * Caso de excepcion: se intenta restaurar una marca que no existe.
     * Verifica que se lanceNotFoundException antes de intentar la restauracion.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenBrandDoesNotExist() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(restoreBrandUseCase.restoreBrand(brandId))
                .expectError(NotFoundException.class)
                .verify();
    }
}
