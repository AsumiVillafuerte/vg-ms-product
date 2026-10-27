package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.memory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
@Profile("mock")
public class InMemoryBrandRepository implements IBrandRepository {

    private final MockDataStore store;

    public InMemoryBrandRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public Mono<Brand> save(Brand brand) {
        return Mono.fromSupplier(() -> {
            if (brand.getId() == null) {
                brand.setId(UUID.randomUUID());
            }
            if (brand.getStatus() == null) {
                brand.setStatus('A');
            }
            store.brands().put(brand.getId(), brand);
            return brand;
        });
    }

    @Override
    public Mono<Brand> findById(UUID id, String organizationId) {
        return Mono.justOrEmpty(store.brands().get(id))
                .filter(brand -> Objects.equals(brand.getOrganizationId(), organizationId));
    }

    @Override
    public Flux<Brand> findAll(String organizationId) {
        return Flux.fromIterable(new ArrayList<>(store.brands().values()))
                .filter(brand -> Objects.equals(brand.getOrganizationId(), organizationId));
    }

    @Override
    public Flux<Brand> findByStatus(Character status, String organizationId) {
        return findAll(organizationId)
                .filter(brand -> Objects.equals(brand.getStatus(), status));
    }

    @Override
    public Mono<Brand> update(Brand brand) {
        return save(brand);
    }

    @Override
    public Mono<Boolean> existsByName(String name, String organizationId) {
        return existsByName(name, organizationId, null);
    }

    @Override
    public Mono<Boolean> existsByName(String name, String organizationId, UUID excludeId) {
        String normalized = normalize(name);
        return findAll(organizationId)
                .filter(brand -> excludeId == null || !excludeId.equals(brand.getId()))
                .any(brand -> normalize(brand.getName()).equals(normalized));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
