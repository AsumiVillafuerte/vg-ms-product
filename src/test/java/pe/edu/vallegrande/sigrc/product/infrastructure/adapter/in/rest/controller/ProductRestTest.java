package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IDeleteProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IRestoreProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateProductUseCase;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper.ProductRestMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRestTest {

    @Mock
    private ICreateProductUseCase createProductUseCase;
    @Mock
    private IGetProductUseCase getProductUseCase;
    @Mock
    private IUpdateProductUseCase updateProductUseCase;
    @Mock
    private IDeleteProductUseCase deleteProductUseCase;
    @Mock
    private IRestoreProductUseCase restoreProductUseCase;

    private ProductRestMapper restMapper;
    private ProductRest controller;

    private UUID productId;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        restMapper = new ProductRestMapper();
        controller = new ProductRest(createProductUseCase, getProductUseCase,
                updateProductUseCase, deleteProductUseCase, restoreProductUseCase, restMapper);
        productId = UUID.randomUUID();
        productResponse = ProductResponse.builder()
                .id(productId)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .productType("venta")
                .status('A')
                .build();
    }

    @Test
    void createProductWrapsResponseWith201() {
        UUID brandId = UUID.randomUUID();
        CreateProductRestRequest request = new CreateProductRestRequest("Laptop HP", brandId, "venta");
        CreateProductRequest applicationRequest = new CreateProductRequest("Laptop HP", brandId, "venta");
        when(createProductUseCase.createProduct(applicationRequest)).thenReturn(Mono.just(productResponse));

        StepVerifier.create(controller.createProduct(request))
                .assertNext(response -> {
                    assertEquals(201, response.getStatus());
                    assertEquals("Producto creado exitosamente", response.getMessage());
                    assertEquals(restMapper.toRest(productResponse), response.getData());
                })
                .verifyComplete();
    }

    @Test
    void getProductByIdWrapsResponseWith200() {
        when(getProductUseCase.getProductById(productId)).thenReturn(Mono.just(productResponse));

        StepVerifier.create(controller.getProductById(productId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Producto encontrado", response.getMessage());
                })
                .verifyComplete();

        verify(getProductUseCase).getProductById(productId);
    }

    @Test
    void getAllProductsCollectsList() {
        when(getProductUseCase.getAllProducts()).thenReturn(Flux.just(productResponse, productResponse));

        StepVerifier.create(controller.getAllProducts())
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Lista de productos", response.getMessage());
                    assertEquals(2, response.getData().size());
                })
                .verifyComplete();
    }

    @Test
    void getProductsByStatusBuildsMessageWithStatus() {
        when(getProductUseCase.getProductsByStatus('A')).thenReturn(Flux.just(productResponse));

        StepVerifier.create(controller.getProductsByStatus('A'))
                .assertNext(response -> {
                    assertEquals("Productos filtrados por estado: A", response.getMessage());
                    assertEquals(1, response.getData().size());
                })
                .verifyComplete();
    }

    @Test
    void updateProductWrapsResponseWith200() {
        UpdateProductRestRequest request = new UpdateProductRestRequest("Teclado", null, null);
        UpdateProductRequest applicationRequest = new UpdateProductRequest("Teclado", null, null);
        when(updateProductUseCase.updateProduct(productId, applicationRequest)).thenReturn(Mono.just(productResponse));

        StepVerifier.create(controller.updateProduct(productId, request))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Producto actualizado exitosamente", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    void deactivateProductReturnsOkWithNullData() {
        when(deleteProductUseCase.logicalDeleteProduct(productId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deactivateProduct(productId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Producto desactivado exitosamente", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        verify(deleteProductUseCase).logicalDeleteProduct(productId);
    }

    @Test
    void activateProductReturnsOkWithNullData() {
        when(restoreProductUseCase.restoreProduct(productId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.activateProduct(productId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Producto reactivado exitosamente", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        verify(restoreProductUseCase).restoreProduct(productId);
    }
}
