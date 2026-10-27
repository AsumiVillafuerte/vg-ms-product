package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para
 * Valida la consulta de productos por ID, listado completo,
 * filtrado por estado y resolucion del nombre de la marca.
 */
@ExtendWith(MockitoExtension.class)
class GetProductUseCaseImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private GetProductUseCaseImpl getProductUseCase;

    private UUID productId;
    private UUID brandId;
    private Product product;
    private Brand brand;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        brandId = UUID.randomUUID();
        product = Product.builder()
                .id(productId).commercialName("Laptop HP").brandId(brandId)
                .productType("venta").status('A')
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        brand = Brand.builder()
                .id(brandId).name("Samsung").status('A').build();
        productResponse = ProductResponse.builder()
                .id(productId).commercialName("Laptop HP").brandId(brandId).brandName("Samsung")
                .productType("venta").status('A').build();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
    }

    /**
     * Caso positivo: consulta un producto por ID con su marca resuelta.
     * Verifica que se retorne la respuesta con el nombre de la marca.
     */
    @Test
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(product));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(productMapper.toResponse(product, "Samsung")).thenReturn(productResponse);

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(productId, response.getId());
                    assertEquals("Laptop HP", response.getCommercialName());
                    assertEquals("Samsung", response.getBrandName());
                })
                .verifyComplete();

        verify(brandRepository).findById(brandId, "org-1");
    }

    /**
     * Caso negativo: no existe un producto con el ID consultado.
     * Verifica que se emita un {@link NotFoundException}.
     */
    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .expectErrorMatches(error -> error instanceof NotFoundException
                        && error.getMessage().contains("Producto no encontrado"))
                .verify();
    }

    /**
     * Caso especial: el producto no tiene marca asociada (brandId nulo).
     * Verifica que se use un nombre de marca vacio sin consultar el repositorio.
     */
    @Test
    void shouldReturnEmptyBrandNameWhenBrandIdIsNull() {
        product.setBrandId(null);
        ProductResponse withoutBrand = ProductResponse.builder()
                .id(productId).commercialName("Laptop HP").brandName("")
                .productType("venta").status('A').build();
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(product));
        when(productMapper.toResponse(product, "")).thenReturn(withoutBrand);

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .assertNext(response -> assertEquals("", response.getBrandName()))
                .verifyComplete();

        verify(brandRepository, never()).findById(any(UUID.class), anyString());
    }

    /**
     * Caso de excepcion: falla la consulta de la marca.
     * Verifica que el error se acuse de recibo y se use un nombre de marca vacio.
     */
    @Test
    void shouldReturnEmptyBrandNameWhenBrandLookupFails() {
        ProductResponse withoutBrand = ProductResponse.builder()
                .id(productId).commercialName("Laptop HP").brandName("")
                .productType("venta").status('A').build();
        when(productRepository.findById(productId, "org-1")).thenReturn(Mono.just(product));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.error(new RuntimeException("error")));
        when(productMapper.toResponse(product, "")).thenReturn(withoutBrand);

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .assertNext(response -> assertEquals("", response.getBrandName()))
                .verifyComplete();
    }

    /**
     * Caso positivo: lista todos los productos con el nombre de su marca.
     * Verifica que se emitan todas las respuestas en orden.
     */
    @Test
    void shouldListAllProducts() {
        Product secondProduct = Product.builder()
                .id(UUID.randomUUID()).commercialName("Mouse").brandId(brandId)
                .productType("venta").status('A').build();
        ProductResponse secondResponse = ProductResponse.builder()
                .id(secondProduct.getId()).commercialName("Mouse").brandId(brandId).brandName("Samsung")
                .productType("venta").status('A').build();

        when(productRepository.findAll("org-1")).thenReturn(Flux.just(product, secondProduct));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(productMapper.toResponse(product, "Samsung")).thenReturn(productResponse);
        when(productMapper.toResponse(secondProduct, "Samsung")).thenReturn(secondResponse);

        StepVerifier.create(getProductUseCase.getAllProducts())
                .expectNext(productResponse, secondResponse)
                .verifyComplete();
    }

    /**
     * Caso positivo: lista productos filtrando por estado.
     * Verifica que la consulta use el estado solicitado.
     */
    @Test
    void shouldListProductsByStatus() {
        when(productRepository.findByStatus('A', "org-1")).thenReturn(Flux.just(product));
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(productMapper.toResponse(product, "Samsung")).thenReturn(productResponse);

        StepVerifier.create(getProductUseCase.getProductsByStatus('A'))
                .expectNext(productResponse)
                .verifyComplete();

        verify(productRepository).findByStatus('A', "org-1");
    }
}