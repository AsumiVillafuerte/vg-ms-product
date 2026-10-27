package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper;

import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.BrandEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrandPersistenceMapperTest {

    private final BrandPersistenceMapper mapper = new BrandPersistenceMapper();

    @Test
    void toDomainCopiesAllFields() {
        UUID id = UUID.randomUUID();
        BrandEntity entity = BrandEntity.builder()
                .id(id)
                .organizationId("org-1")
                .name("Samsung")
                .status("I")
                .build();

        Brand brand = mapper.toDomain(entity);

        assertEquals(id, brand.getId());
        assertEquals("org-1", brand.getOrganizationId());
        assertEquals("Samsung", brand.getName());
        assertEquals('I', brand.getStatus());
    }

    @Test
    void toDomainDefaultsStatusToActiveWhenNull() {
        BrandEntity entity = BrandEntity.builder().name("Samsung").status(null).build();

        assertEquals('A', mapper.toDomain(entity).getStatus());
    }

    @Test
    void toEntityCopiesAllFields() {
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder()
                .id(id)
                .organizationId("org-1")
                .name("Samsung")
                .status('A')
                .build();

        BrandEntity entity = mapper.toEntity(brand);

        assertEquals(id, entity.getId());
        assertEquals("org-1", entity.getOrganizationId());
        assertEquals("Samsung", entity.getName());
        assertEquals("A", entity.getStatus());
    }

    @Test
    void toEntityDefaultsStatusToActiveWhenNull() {
        Brand brand = Brand.builder().name("Samsung").status(null).build();

        BrandEntity entity = mapper.toEntity(brand);

        assertEquals("A", entity.getStatus());
        assertNull(entity.getId());
    }
}
