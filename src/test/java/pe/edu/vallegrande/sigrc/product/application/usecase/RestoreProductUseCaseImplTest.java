package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link RestoreProductUseCaseImpl}.
 * Valida la restauracion de productos inactivos, el caso de un
 * producto ya activo y el manejo de producto inexistente.
 */
@ExtendWith(MockitoExtension.class)
class RestoreProductUseCaseImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private RestoreProductUseCaseImpl restoreProductUseCase;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    /**
     * Caso positivo: restaura un producto inactivo cambiando su estado a 'A'.
     * Verifica que el repositorio reciba la actualizacion con estado activo.
     */
    @Test
    void shouldRestoreProductSuccessfully() {
        Product inactiveProduct = Product.builder()
                .id(productId).commercialName("Laptop HP")
                .productType("venta").status('I')
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5)).build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(inactiveProduct));
        when(productRepository.update(any(Product.class))).thenReturn(Mono.just(inactiveProduct));

        StepVerifier.create(restoreProductUseCase.restoreProduct(productId))
                .verifyComplete();

        verify(productRepository).update(argThat(p -> p.getStatus().equals('A')));
    }

    /**
     * Caso negativo: se intenta restaurar un producto que ya esta activo.
     * Verifica que la operacion se complete sin error, manteniendo el estado 'A'.
     */
    @Test
    void shouldHandleRestoreOfAlreadyActiveProduct() {
        Product activeProduct = Product.builder()
                .id(productId).commercialName("Laptop Dell").status('A').build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(activeProduct));
        when(productRepository.update(any(Product.class))).thenReturn(Mono.just(activeProduct));

        StepVerifier.create(restoreProductUseCase.restoreProduct(productId))
                .verifyComplete();
    }

    /**
     * Caso de excepcion: se intenta restaurar un producto que no existe.
     * Verifica que se lance NotFoundException antes de intentar la restauracion.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenProductDoesNotExist() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(restoreProductUseCase.restoreProduct(productId))
                .expectError(NotFoundException.class)
                .verify();
    }
}
