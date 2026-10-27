package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.ProductEntity;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper.ProductPersistenceMapper;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository.ProductR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPersistenceAdapterTest {

    @Mock
    private ProductR2dbcRepository r2dbcRepository;

    private final ProductPersistenceMapper mapper = new ProductPersistenceMapper();

    private ProductPersistenceAdapter repository;

    private UUID productId;
    private ProductEntity entity;

    @BeforeEach
    void setUp() {
        repository = new ProductPersistenceAdapter(r2dbcRepository, mapper);
        productId = UUID.randomUUID();
        entity = ProductEntity.builder()
                .id(productId)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .productType("venta")
                .status("A")
                .build();
    }

    private Product domain() {
        return Product.builder()
                .id(productId)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .productType("venta")
                .status('A')
                .build();
    }

    @Test
    void savePersistsAndMapsBackToDomain() {
        when(r2dbcRepository.save(any(ProductEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.save(domain()))
                .assertNext(product -> {
                    assertEquals(productId, product.getId());
                    assertEquals("Laptop HP", product.getCommercialName());
                    assertEquals('A', product.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsDomainWhenPresent() {
        when(r2dbcRepository.findByIdAndOrganizationId(productId, "org-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.findById(productId, "org-1"))
                .assertNext(product -> assertEquals(productId, product.getId()))
                .verifyComplete();

        verify(r2dbcRepository).findByIdAndOrganizationId(productId, "org-1");
    }

    @Test
    void findByIdCompletesEmptyWhenNotFound() {
        when(r2dbcRepository.findByIdAndOrganizationId(productId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById(productId, "org-1")).verifyComplete();
    }

    @Test
    void findAllMapsEveryElement() {
        when(r2dbcRepository.findByOrganizationId("org-1")).thenReturn(Flux.just(entity, entity));

        StepVerifier.create(repository.findAll("org-1"))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByStatusUsesStringValueOfStatus() {
        when(r2dbcRepository.findByOrganizationIdAndStatus("org-1", "A")).thenReturn(Flux.just(entity));

        StepVerifier.create(repository.findByStatus('A', "org-1"))
                .assertNext(product -> assertEquals('A', product.getStatus()))
                .verifyComplete();

        verify(r2dbcRepository).findByOrganizationIdAndStatus("org-1", "A");
    }

    @Test
    void updatePersistsAndMapsBackToDomain() {
        when(r2dbcRepository.save(any(ProductEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.update(domain()))
                .assertNext(product -> assertEquals(productId, product.getId()))
                .verifyComplete();
    }

    @Test
    void propagatesRepositoryErrorOnSave() {
        when(r2dbcRepository.save(any(ProductEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("db down")));

        StepVerifier.create(repository.save(domain()))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void propagatesRepositoryErrorOnFind() {
        when(r2dbcRepository.findByIdAndOrganizationId(any(UUID.class), anyString()))
                .thenReturn(Mono.error(new RuntimeException("db down")));

        StepVerifier.create(repository.findById(productId, "org-1"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
