package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper.BrandPersistenceMapper;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository.BrandR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Profile("!mock")
public class BrandPersistenceAdapter implements IBrandRepository {

    private final BrandR2dbcRepository r2dbcRepository;
    private final BrandPersistenceMapper mapper;

    public BrandPersistenceAdapter(BrandR2dbcRepository r2dbcRepository,
                                   BrandPersistenceMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Brand> save(Brand brand) {
        return r2dbcRepository.save(mapper.toEntity(brand))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Brand> findById(UUID id, String organizationId) {
        return r2dbcRepository.findByIdAndOrganizationId(id, organizationId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Brand> findAll(String organizationId) {
        return r2dbcRepository.findByOrganizationId(organizationId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Brand> findByStatus(Character status, String organizationId) {
        return r2dbcRepository.findByOrganizationIdAndStatus(organizationId, String.valueOf(status))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Brand> update(Brand brand) {
        return r2dbcRepository.save(mapper.toEntity(brand))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByName(String name, String organizationId) {
        return r2dbcRepository.existsByName(name, organizationId);
    }

    @Override
    public Mono<Boolean> existsByName(String name, String organizationId, UUID excludeId) {
        return r2dbcRepository.existsByName(name, organizationId, excludeId);
    }
}
