package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IDeleteProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IRestoreProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateProductUseCase;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.ProductRestResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper.ProductRestMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductRest {

    private final ICreateProductUseCase createProductUseCase;
    private final IGetProductUseCase getProductUseCase;
    private final IUpdateProductUseCase updateProductUseCase;
    private final IDeleteProductUseCase deleteProductUseCase;
    private final IRestoreProductUseCase restoreProductUseCase;
    private final ProductRestMapper restMapper;

    public ProductRest(ICreateProductUseCase createProductUseCase,
                       IGetProductUseCase getProductUseCase,
                       IUpdateProductUseCase updateProductUseCase,
                       IDeleteProductUseCase deleteProductUseCase,
                       IRestoreProductUseCase restoreProductUseCase,
                       ProductRestMapper restMapper) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.restoreProductUseCase = restoreProductUseCase;
        this.restMapper = restMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ProductRestResponse>> createProduct(
            @Valid @RequestBody CreateProductRestRequest request) {
        return createProductUseCase.createProduct(restMapper.toApplication(request))
                .map(restMapper::toRest)
                .map(product -> new ApiResponse<>(HttpStatus.CREATED.value(), "Producto creado exitosamente", product));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<ProductRestResponse>> getProductById(@PathVariable UUID id) {
        return getProductUseCase.getProductById(id)
                .map(restMapper::toRest)
                .map(product -> new ApiResponse<>(HttpStatus.OK.value(), "Producto encontrado", product));
    }

    @GetMapping
    public Mono<ApiResponse<List<ProductRestResponse>>> getAllProducts() {
        return getProductUseCase.getAllProducts()
                .map(restMapper::toRest)
                .collectList()
                .map(products -> new ApiResponse<>(HttpStatus.OK.value(), "Lista de productos", products));
    }

    @GetMapping("/status/{status}")
    public Mono<ApiResponse<List<ProductRestResponse>>> getProductsByStatus(@PathVariable Character status) {
        return getProductUseCase.getProductsByStatus(status)
                .map(restMapper::toRest)
                .collectList()
                .map(products -> new ApiResponse<>(HttpStatus.OK.value(), "Productos filtrados por estado: " + status, products));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<ProductRestResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRestRequest request) {
        return updateProductUseCase.updateProduct(id, restMapper.toApplication(request))
                .map(restMapper::toRest)
                .map(product -> new ApiResponse<>(HttpStatus.OK.value(), "Producto actualizado exitosamente", product));
    }

    @PatchMapping("/{id}/deactivate")
    public Mono<ApiResponse<Void>> deactivateProduct(@PathVariable UUID id) {
        return deleteProductUseCase.logicalDeleteProduct(id)
                .then(Mono.just(new ApiResponse<>(HttpStatus.OK.value(), "Producto desactivado exitosamente", null)));
    }

    @PatchMapping("/{id}/activate")
    public Mono<ApiResponse<Void>> activateProduct(@PathVariable UUID id) {
        return restoreProductUseCase.restoreProduct(id)
                .then(Mono.just(new ApiResponse<>(HttpStatus.OK.value(), "Producto reactivado exitosamente", null)));
    }
}
