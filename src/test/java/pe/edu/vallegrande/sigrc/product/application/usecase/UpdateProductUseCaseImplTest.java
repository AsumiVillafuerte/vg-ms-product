package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link UpdateProductUseCaseImpl}.
 * Valida la actualizacion de productos, incluyendo actualizacion
 * completa, conservacion de valores existentes y producto inexistente.
 */
@ExtendWith(MockitoExtension.class)
class UpdateProductUseCaseImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private UpdateProductUseCaseImpl updateProductUseCase;

    private UUID productId;
    private UUID brandId;
    private Product existingProduct;
    private Brand brand;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        brandId = UUID.randomUUID();
        existingProduct = Product.builder()
                .id(productId).commercialName("Laptop HP").brandId(brandId)
                .productType("venta").status('A')
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(5)).build();
        brand = Brand.builder().id(brandId).name("Samsung").status('A').build();
        productResponse = ProductResponse.builder()
                .id(productId).commercialName("Laptop HP Pro").brandId(brandId).brandName("Samsung")
                .productType("venta").status('A').build();
    }

    /**
     * Caso positivo: actualiza todos los campos de un producto existente.
     * Verifica que se apliquen los nuevos valores y se retorne la respuesta actualizada.
     */
    @Test
    void shouldUpdateProductSuccessfully() {
        UpdateProductRequest request = new UpdateProductRequest("Laptop HP Pro", brandId, "venta");
        Product mappedProduct = Product.builder()
                .commercialName("Laptop HP Pro").brandId(brandId).productType("venta").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(existingProduct));
        when(productMapper.mapUpdateToDomain(request)).thenReturn(mappedProduct);
        when(productRepository.update(any(Product.class))).thenReturn(Mono.just(mappedProduct));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(productMapper.toResponse(mappedProduct, "Samsung")).thenReturn(productResponse);

        StepVerifier.create(updateProductUseCase.updateProduct(productId, request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("Laptop HP Pro", response.getCommercialName());
                    assertEquals("venta", response.getProductType());
                    assertEquals("Samsung", response.getBrandName());
                })
                .verifyComplete();
    }

    /**
     * Caso negativo: el request tiene todos los campos nulos, por lo que se
     * conservan los valores originales del producto.
     * Verifica que ningun campo se sobreescriba con null.
     */
    @Test
    void shouldKeepExistingValuesWhenFieldsAreNull() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, null);
        Product mappedProduct = Product.builder().build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(existingProduct));
        when(productMapper.mapUpdateToDomain(request)).thenReturn(mappedProduct);
        when(productRepository.update(any(Product.class))).thenReturn(Mono.just(existingProduct));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(productMapper.toResponse(existingProduct, "Samsung")).thenReturn(productResponse);

        StepVerifier.create(updateProductUseCase.updateProduct(productId, request))
                .assertNext(response -> assertNotNull(response))
                .verifyComplete();

        verify(productRepository).update(argThat(p ->
                p.getCommercialName().equals("Laptop HP") &&
                p.getProductType().equals("venta")));
    }

    /**
     * Caso de excepcion: se intenta actualizar un producto que no existe.
     * Verifica que se lance NotFoundException antes de intentar la actualizacion.
     */
    @Test
    void shouldThrowNotFoundExceptionWhenProductDoesNotExist() {
        UpdateProductRequest request = new UpdateProductRequest("Nuevo", null, null);

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(updateProductUseCase.updateProduct(productId, request))
                .expectError(NotFoundException.class)
                .verify();
    }
}
