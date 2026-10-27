package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper;

import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.ProductEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductPersistenceMapperTest {

    private final ProductPersistenceMapper mapper = new ProductPersistenceMapper();

    @Test
    void toDomainCopiesAllFields() {
        UUID id = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ProductEntity entity = ProductEntity.builder()
                .id(id)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .brandId(brandId)
                .productType("venta")
                .status("I")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Product product = mapper.toDomain(entity);

        assertEquals(id, product.getId());
        assertEquals("org-1", product.getOrganizationId());
        assertEquals("Laptop HP", product.getCommercialName());
        assertEquals(brandId, product.getBrandId());
        assertEquals("venta", product.getProductType());
        assertEquals('I', product.getStatus());
        assertEquals(now, product.getCreatedAt());
        assertEquals(now, product.getUpdatedAt());
    }

    @Test
    void toDomainDefaultsStatusToActiveWhenNull() {
        ProductEntity entity = ProductEntity.builder().commercialName("Mouse").status(null).build();

        assertEquals('A', mapper.toDomain(entity).getStatus());
    }

    @Test
    void toEntityCopiesAllFields() {
        UUID id = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .id(id)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .brandId(brandId)
                .productType("donado")
                .status('A')
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProductEntity entity = mapper.toEntity(product);

        assertEquals(id, entity.getId());
        assertEquals("org-1", entity.getOrganizationId());
        assertEquals("Laptop HP", entity.getCommercialName());
        assertEquals(brandId, entity.getBrandId());
        assertEquals("donado", entity.getProductType());
        assertEquals("A", entity.getStatus());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void toEntityDefaultsStatusToActiveWhenNull() {
        Product product = Product.builder().commercialName("Mouse").status(null).build();

        ProductEntity entity = mapper.toEntity(product);

        assertEquals("A", entity.getStatus());
        assertNull(entity.getId());
    }
}
