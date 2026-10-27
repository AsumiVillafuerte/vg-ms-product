package pe.edu.vallegrande.sigrc.product.application.mapper;

import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BrandMapperTest {

    private final BrandMapper mapper = new BrandMapper();

    @Test
    void toDomainMapsNameAndActivatesBrand() {
        Brand brand = mapper.toDomain(new CreateBrandRequest("Samsung"));

        assertEquals("Samsung", brand.getName());
        assertEquals('A', brand.getStatus());
    }

    @Test
    void mapUpdateToDomainMapsOnlyName() {
        Brand brand = mapper.mapUpdateToDomain(new UpdateBrandRequest("LG"));

        assertEquals("LG", brand.getName());
        assertNull(brand.getStatus());
    }

    @Test
    void toResponseCopiesAllFields() {
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder()
                .id(id)
                .organizationId("org-1")
                .name("Samsung")
                .status('A')
                .build();

        BrandResponse response = mapper.toResponse(brand);

        assertEquals(id, response.getId());
        assertEquals("org-1", response.getOrganizationId());
        assertEquals("Samsung", response.getName());
        assertEquals('A', response.getStatus());
    }
}
