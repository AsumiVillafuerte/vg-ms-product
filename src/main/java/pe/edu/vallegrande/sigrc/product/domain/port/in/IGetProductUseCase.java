package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IGetProductUseCase {
    Mono<ProductResponse> getProductById(UUID id);
    Flux<ProductResponse> getAllProducts();
    Flux<ProductResponse> getProductsByStatus(Character status);
}
