package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.exception.UnauthorizedException;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Aislamiento multi-tenant entre dos organizaciones y dos usuarios.
 *
 * <p>El organizationId se toma siempre del token (proveedor), nunca del cuerpo de la
 * peticion, y todas las consultas se filtran por ese valor. Se comprueba que un usuario
 * de la organizacion B no puede leer/modificar/borrar productos de la organizacion A.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductOrganizationIsolationTest {

    private static final String ORG_A = "org-A";
    private static final String ORG_B = "org-B";

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    private CreateProductUseCaseImpl createProductUseCase;
    private GetProductUseCaseImpl getProductUseCase;
    private UpdateProductUseCaseImpl updateProductUseCase;
    private DeleteProductUseCaseImpl deleteProductUseCase;
    private RestoreProductUseCaseImpl restoreProductUseCase;

    @BeforeEach
    void setUp() {
        createProductUseCase = new CreateProductUseCaseImpl(
                productRepository, brandRepository, productMapper, currentUserProvider);
        getProductUseCase = new GetProductUseCaseImpl(
                productRepository, brandRepository, productMapper, currentUserProvider);
        updateProductUseCase = new UpdateProductUseCaseImpl(
                productRepository, brandRepository, productMapper, currentUserProvider);
        deleteProductUseCase = new DeleteProductUseCaseImpl(
                productRepository, currentUserProvider);
        restoreProductUseCase = new RestoreProductUseCaseImpl(
                productRepository, currentUserProvider);
    }

    @Test
    void createStampsOrganizationFromToken() {
        UUID productId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Laptop HP", null, "venta");
        Product domain = Product.builder().id(productId).commercialName("Laptop HP").productType("venta").build();
        ProductResponse response = ProductResponse.builder().id(productId).commercialName("Laptop HP").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_A));
        when(productMapper.toDomain(request)).thenReturn(domain);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(productMapper.toResponse(any(Product.class), anyString())).thenReturn(response);

        StepVerifier.create(createProductUseCase.createProduct(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        assertEquals(ORG_A, savedCaptor.getValue().getOrganizationId());
    }

    @Test
    void userFromOrgBCannotReadProductFromOrgA() {
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(productRepository.findById(productId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .expectError(NotFoundException.class)
                .verify();

        verify(productRepository).findById(productId, ORG_B);
        verify(productRepository, never()).findById(productId, ORG_A);
        verify(productMapper, never()).toResponse(any(Product.class), anyString());
    }

    @Test
    void userFromOrgBCannotUpdateProductFromOrgA() {
        UUID productId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest("Hack", null, "venta");
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(productRepository.findById(productId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(updateProductUseCase.updateProduct(productId, request))
                .expectError(NotFoundException.class)
                .verify();

        verify(productRepository, never()).update(any(Product.class));
    }

    @Test
    void userFromOrgBCannotDeleteProductFromOrgA() {
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(productRepository.findById(productId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(deleteProductUseCase.logicalDeleteProduct(productId))
                .expectError(NotFoundException.class)
                .verify();

        verify(productRepository, never()).update(any(Product.class));
    }

    @Test
    void userFromOrgBCannotRestoreProductFromOrgA() {
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(productRepository.findById(productId, ORG_B)).thenReturn(Mono.empty());

        StepVerifier.create(restoreProductUseCase.restoreProduct(productId))
                .expectError(NotFoundException.class)
                .verify();

        verify(productRepository, never()).update(any(Product.class));
    }

    @Test
    void getAllProductsOnlyQueriesCallerOrganization() {
        UUID productId = UUID.randomUUID();
        Product productB = Product.builder().id(productId).organizationId(ORG_B).commercialName("Mouse").build();
        ProductResponse response = ProductResponse.builder().id(productId).organizationId(ORG_B).commercialName("Mouse").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_B));
        when(productRepository.findAll(ORG_B)).thenReturn(Flux.just(productB));
        when(productMapper.toResponse(productB, "")).thenReturn(response);

        StepVerifier.create(getProductUseCase.getAllProducts())
                .expectNext(response)
                .verifyComplete();

        verify(productRepository).findAll(ORG_B);
        verify(productRepository, never()).findAll(ORG_A);
    }

    @Test
    void createOverwritesAnyOrganizationOnMappedDomain() {
        UUID productId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Laptop", null, "venta");
        Product forged = Product.builder().id(productId).organizationId(ORG_B).commercialName("Laptop").productType("venta").build();
        ProductResponse response = ProductResponse.builder().id(productId).commercialName("Laptop").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_A));
        when(productMapper.toDomain(request)).thenReturn(forged);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(productMapper.toResponse(any(Product.class), anyString())).thenReturn(response);

        StepVerifier.create(createProductUseCase.createProduct(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(savedCaptor.capture());
        assertEquals(ORG_A, savedCaptor.getValue().getOrganizationId());
    }

    @Test
    void updateKeepsExistingOrganization() {
        UUID productId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest("Laptop HP", null, "venta");
        Product existing = Product.builder().id(productId).organizationId(ORG_A)
                .commercialName("Laptop").productType("venta").status('A').build();
        Product forged = Product.builder().id(productId).organizationId(ORG_B)
                .commercialName("Laptop HP").productType("venta").status('I').build();
        ProductResponse response = ProductResponse.builder().id(productId).commercialName("Laptop HP").build();

        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just(ORG_A));
        when(productRepository.findById(productId, ORG_A)).thenReturn(Mono.just(existing));
        when(productMapper.mapUpdateToDomain(request)).thenReturn(forged);
        when(productRepository.update(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(productMapper.toResponse(any(Product.class), anyString())).thenReturn(response);

        StepVerifier.create(updateProductUseCase.updateProduct(productId, request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Product> savedCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).update(savedCaptor.capture());
        assertEquals(ORG_A, savedCaptor.getValue().getOrganizationId());
    }

    @Test
    void missingOrganizationFailsClosedWithoutQueryingRepository() {
        UUID productId = UUID.randomUUID();
        when(currentUserProvider.getOrganizationId())
                .thenReturn(Mono.error(new UnauthorizedException("sin organizacion")));

        StepVerifier.create(getProductUseCase.getProductById(productId))
                .expectError(UnauthorizedException.class)
                .verify();

        verify(productRepository, never()).findById(eq(productId), anyString());
        verify(productRepository, never()).findAll(anyString());
        verify(productRepository, never()).save(any(Product.class));
    }
}
