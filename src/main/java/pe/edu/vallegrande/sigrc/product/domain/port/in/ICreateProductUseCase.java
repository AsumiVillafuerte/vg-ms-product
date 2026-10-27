package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import reactor.core.publisher.Mono;

public interface ICreateProductUseCase {
    Mono<ProductResponse> createProduct(CreateProductRequest request);
}
