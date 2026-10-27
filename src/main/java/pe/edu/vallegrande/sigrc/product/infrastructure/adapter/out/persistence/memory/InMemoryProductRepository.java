package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.memory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Component
@Profile("mock")
public class InMemoryProductRepository implements IProductRepository {

    private final MockDataStore store;

    public InMemoryProductRepository(MockDataStore store) {
        this.store = store;
    }

    @Override
    public Mono<Product> save(Product product) {
        return Mono.fromSupplier(() -> {
            LocalDateTime now = LocalDateTime.now();
            if (product.getId() == null) {
                product.setId(UUID.randomUUID());
            }
            if (product.getStatus() == null) {
                product.setStatus('A');
            }
            if (product.getCreatedAt() == null) {
                product.setCreatedAt(now);
            }
            product.setUpdatedAt(now);
            store.products().put(product.getId(), product);
            return product;
        });
    }

    @Override
    public Mono<Product> findById(UUID id, String organizationId) {
        return Mono.justOrEmpty(store.products().get(id))
                .filter(product -> Objects.equals(product.getOrganizationId(), organizationId));
    }

    @Override
    public Flux<Product> findAll(String organizationId) {
        return Flux.fromIterable(new ArrayList<>(store.products().values()))
                .filter(product -> Objects.equals(product.getOrganizationId(), organizationId));
    }

    @Override
    public Flux<Product> findByStatus(Character status, String organizationId) {
        return findAll(organizationId)
                .filter(product -> Objects.equals(product.getStatus(), status));
    }

    @Override
    public Mono<Product> update(Product product) {
        return Mono.fromSupplier(() -> {
            product.setUpdatedAt(LocalDateTime.now());
            store.products().put(product.getId(), product);
            return product;
        });
    }
}
