package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.ProductEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductR2dbcRepository extends R2dbcRepository<ProductEntity, UUID> {
    Mono<ProductEntity> findByIdAndOrganizationId(UUID id, String organizationId);
    Flux<ProductEntity> findByOrganizationId(String organizationId);
    Flux<ProductEntity> findByOrganizationIdAndStatus(String organizationId, String status);
}
