package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
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
 * Tests unitarios para {@link CreateProductUseCaseImpl}.
 * Valida la creacion de productos cubriendo el caso exitoso
 * y errores del repositorio.
 */
@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private CreateProductUseCaseImpl createProductUseCase;

    private CreateProductRequest request;
    private Product productDomain;
    private ProductResponse productResponse;
    private UUID productId;
    private UUID brandId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        brandId = UUID.randomUUID();
        request = new CreateProductRequest("Laptop HP", brandId, "venta");
        productDomain = Product.builder()
                .id(productId).commercialName("Laptop HP").brandId(brandId)
                .productType("venta").status('A')
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        productResponse = ProductResponse.builder()
                .id(productId).commercialName("Laptop HP").brandId(brandId).brandName("Samsung")
                .productType("venta").status('A').build();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
    }

    /**
     * Caso positivo: crea un producto con todos los campos validos.
     * Verifica que se retorne la respuesta con todos los datos correctamente mapeados.
     */
    @Test
    void shouldCreateProductSuccessfully() {
        when(productMapper.toDomain(request)).thenReturn(productDomain);
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(productDomain));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(
                Brand.builder().id(brandId).name("Samsung").status('A').build()));
        when(productMapper.toResponse(productDomain, "Samsung")).thenReturn(productResponse);

        StepVerifier.create(createProductUseCase.createProduct(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(productId, response.getId());
                    assertEquals("Laptop HP", response.getCommercialName());
                    assertEquals("venta", response.getProductType());
                    assertEquals("Samsung", response.getBrandName());
                })
                .verifyComplete();
    }

    /**
     * Caso de excepcion: el repositorio falla por error de conexion.
     * Verifica que la excepcion se propague sin ser capturada por el caso de uso.
     */
    @Test
    void shouldReturnErrorWhenRepositoryFails() {
        when(productMapper.toDomain(request)).thenReturn(productDomain);
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(
                Brand.builder().id(brandId).name("Samsung").status('A').build()));
        when(productRepository.save(any(Product.class)))
                .thenReturn(Mono.error(new RuntimeException("Error de conexion")));

        StepVerifier.create(createProductUseCase.createProduct(request))
                .expectError(RuntimeException.class)
                .verify();
    }
}
