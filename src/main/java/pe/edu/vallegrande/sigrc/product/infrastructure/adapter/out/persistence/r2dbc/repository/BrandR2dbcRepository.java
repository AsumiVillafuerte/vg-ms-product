package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.BrandEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BrandR2dbcRepository extends R2dbcRepository<BrandEntity, UUID> {
    Mono<BrandEntity> findByIdAndOrganizationId(UUID id, String organizationId);
    Flux<BrandEntity> findByOrganizationId(String organizationId);
    Flux<BrandEntity> findByOrganizationIdAndStatus(String organizationId, String status);

    @Query("""
            SELECT EXISTS(SELECT 1 FROM brands
              WHERE lower(btrim(name)) = lower(btrim(:name))
                AND organization_id = :organizationId)
            """)
    Mono<Boolean> existsByName(@Param("name") String name, @Param("organizationId") String organizationId);

    @Query("""
            SELECT EXISTS(SELECT 1 FROM brands
              WHERE lower(btrim(name)) = lower(btrim(:name))
                AND organization_id = :organizationId
                AND id <> :excludeId)
            """)
    Mono<Boolean> existsByName(@Param("name") String name,
                               @Param("organizationId") String organizationId,
                               @Param("excludeId") UUID excludeId);
}
