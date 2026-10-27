package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper.ProductPersistenceMapper;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Profile("!mock")
public class ProductPersistenceAdapter implements IProductRepository {

    private final ProductR2dbcRepository r2dbcRepository;
    private final ProductPersistenceMapper mapper;

    public ProductPersistenceAdapter(ProductR2dbcRepository r2dbcRepository,
                                     ProductPersistenceMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Product> save(Product product) {
        return r2dbcRepository.save(mapper.toEntity(product))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Product> findById(UUID id, String organizationId) {
        return r2dbcRepository.findByIdAndOrganizationId(id, organizationId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findAll(String organizationId) {
        return r2dbcRepository.findByOrganizationId(organizationId)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Product> findByStatus(Character status, String organizationId) {
        return r2dbcRepository.findByOrganizationIdAndStatus(organizationId, String.valueOf(status))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Product> update(Product product) {
        return r2dbcRepository.save(mapper.toEntity(product))
                .map(mapper::toDomain);
    }
}
