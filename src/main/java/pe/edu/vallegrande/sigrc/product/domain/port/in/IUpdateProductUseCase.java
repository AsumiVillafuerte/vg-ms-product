package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IUpdateProductUseCase {
    Mono<ProductResponse> updateProduct(UUID id, UpdateProductRequest request);
}
