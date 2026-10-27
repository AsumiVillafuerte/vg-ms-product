package pe.edu.vallegrande.sigrc.product.domain.port.out;

import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IProductRepository {
    Mono<Product> save(Product product);
    Mono<Product> findById(UUID id, String organizationId);
    Flux<Product> findAll(String organizationId);
    Flux<Product> findByStatus(Character status, String organizationId);
    Mono<Product> update(Product product);
}
