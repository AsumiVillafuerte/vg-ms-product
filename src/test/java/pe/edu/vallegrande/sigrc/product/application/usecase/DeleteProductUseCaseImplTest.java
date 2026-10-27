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
 * Tests unitarios para {@link DeleteProductUseCaseImpl}.
 * Valida la eliminacion logica de productos y el manejo de producto inexistente.
 */
@ExtendWith(MockitoExtension.class)
class DeleteProductUseCaseImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private DeleteProductUseCaseImpl deleteProductUseCase;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    /**
     * Caso positivo: desactiva logicamente un producto cambiando su estado a 'I'.
     * Verifica que el registro no se elimine fisicamente, solo cambie de estado.
     */
    @Test
    void shouldLogicallyDeleteProductSuccessfully() {
        Product product = Product.builder()
                .id(productId).commercialName("Laptop HP")
                .productType("venta").status('A')
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5)).build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(product));
        when(productRepository.update(any(Product.class))).thenReturn(Mono.just(product));

        StepVerifier.create(deleteProductUseCase.logicalDeleteProduct(productId))
                .verifyComplete();

        verify(productRepository).update(argThat(p -> p.getStatus().equals('I')));
    }

    /**
     * Caso de excepcion: se intenta desactivar un producto que no existe.
     * Verifica que se lance NotFoundException y no se ejecute la actualizacion.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenProductDoesNotExist() {
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(deleteProductUseCase.logicalDeleteProduct(productId))
                .expectError(NotFoundException.class)
                .verify();
    }
}
