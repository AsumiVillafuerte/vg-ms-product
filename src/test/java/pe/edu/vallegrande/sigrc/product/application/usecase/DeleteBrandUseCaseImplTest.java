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
 * Tests unitarios para {@link DeleteBrandUseCaseImpl}.
 * Valida la eliminacion logica de marcas y el manejo de marca inexistente.
 */
@ExtendWith(MockitoExtension.class)
class DeleteBrandUseCaseImplTest {

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private DeleteBrandUseCaseImpl deleteBrandUseCase;

    private UUID brandId;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
    }

    /**
     * Caso positivo: desactiva logicamente una marca cambiando su estado a 'I'.
     * Verifica que el registro no se elimine fisicamente, solo cambie de estado.
     */
    @Test
    void shouldLogicallyDeleteBrandSuccessfully() {
        Brand brand = Brand.builder()
                .id(brandId).name("Samsung").status('A').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(brandRepository.update(any(Brand.class))).thenReturn(Mono.just(brand));

        StepVerifier.create(deleteBrandUseCase.logicalDeleteBrand(brandId))
                .verifyComplete();

        verify(brandRepository).update(argThat(b -> b.getStatus().equals('I')));
    }

    /**
     * Caso de excepcion: se intenta desactivar una marca que no existe.
     * Verifica que se lance NotFoundException y no se ejecute la actualizacion.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenBrandDoesNotExist() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(deleteBrandUseCase.logicalDeleteBrand(brandId))
                .expectError(NotFoundException.class)
                .verify();
    }
}
