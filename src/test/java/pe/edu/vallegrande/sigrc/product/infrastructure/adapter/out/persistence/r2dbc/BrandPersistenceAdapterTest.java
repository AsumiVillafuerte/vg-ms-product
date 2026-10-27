package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.BrandEntity;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper.BrandPersistenceMapper;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.repository.BrandR2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandPersistenceAdapterTest {

    @Mock
    private BrandR2dbcRepository r2dbcRepository;

    private final BrandPersistenceMapper mapper = new BrandPersistenceMapper();

    private BrandPersistenceAdapter repository;

    private UUID brandId;
    private BrandEntity entity;

    @BeforeEach
    void setUp() {
        repository = new BrandPersistenceAdapter(r2dbcRepository, mapper);
        brandId = UUID.randomUUID();
        entity = BrandEntity.builder()
                .id(brandId)
                .organizationId("org-1")
                .name("Samsung")
                .status("A")
                .build();
    }

    private Brand domain() {
        return Brand.builder().id(brandId).organizationId("org-1").name("Samsung").status('A').build();
    }

    @Test
    void savePersistsAndMapsBackToDomain() {
        when(r2dbcRepository.save(any(BrandEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.save(domain()))
                .assertNext(brand -> {
                    assertEquals(brandId, brand.getId());
                    assertEquals("Samsung", brand.getName());
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsDomainWhenPresent() {
        when(r2dbcRepository.findByIdAndOrganizationId(brandId, "org-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.findById(brandId, "org-1"))
                .assertNext(brand -> assertEquals(brandId, brand.getId()))
                .verifyComplete();
    }

    @Test
    void findByIdCompletesEmptyWhenNotFound() {
        when(r2dbcRepository.findByIdAndOrganizationId(brandId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById(brandId, "org-1")).verifyComplete();
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
        when(r2dbcRepository.findByOrganizationIdAndStatus("org-1", "I")).thenReturn(Flux.just(entity));

        StepVerifier.create(repository.findByStatus('I', "org-1"))
                .assertNext(brand -> assertEquals('A', brand.getStatus()))
                .verifyComplete();

        verify(r2dbcRepository).findByOrganizationIdAndStatus("org-1", "I");
    }

    @Test
    void updatePersistsAndMapsBackToDomain() {
        when(r2dbcRepository.save(any(BrandEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(repository.update(domain()))
                .assertNext(brand -> assertEquals(brandId, brand.getId()))
                .verifyComplete();
    }

    @Test
    void existsByNameDelegatesToRepository() {
        when(r2dbcRepository.existsByName("Samsung", "org-1")).thenReturn(Mono.just(true));

        StepVerifier.create(repository.existsByName("Samsung", "org-1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByNameWithExcludeIdDelegatesToRepository() {
        UUID excludeId = UUID.randomUUID();
        when(r2dbcRepository.existsByName("Samsung", "org-1", excludeId)).thenReturn(Mono.just(false));

        StepVerifier.create(repository.existsByName("Samsung", "org-1", excludeId))
                .expectNext(false)
                .verifyComplete();

        verify(r2dbcRepository).existsByName("Samsung", "org-1", excludeId);
    }

    @Test
    void propagatesRepositoryErrorOnSave() {
        when(r2dbcRepository.save(any(BrandEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("db down")));

        StepVerifier.create(repository.save(domain()))
                .expectError(RuntimeException.class)
                .verify();
    }
}
