package pe.edu.vallegrande.sigrc.product.application.mapper;

import org.junit.jupiter.api.Test;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toDomainMapsRequestAndDefaultsStatusToActive() {
        UUID brandId = UUID.randomUUID();
        CreateProductRequest request = new CreateProductRequest("Laptop HP", brandId, "venta");

        Product product = mapper.toDomain(request);

        assertEquals("Laptop HP", product.getCommercialName());
        assertEquals(brandId, product.getBrandId());
        assertEquals("venta", product.getProductType());
        assertEquals('A', product.getStatus());
        assertNotNull(product.getCreatedAt());
        assertNotNull(product.getUpdatedAt());
    }

    @Test
    void toDomainDefaultsProductTypeToVentaWhenNull() {
        CreateProductRequest request = new CreateProductRequest("Mouse", UUID.randomUUID(), null);

        assertEquals("venta", mapper.toDomain(request).getProductType());
    }

    @Test
    void mapUpdateToDomainMapsEditableFields() {
        UUID brandId = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest("Teclado", brandId, "donado");

        Product product = mapper.mapUpdateToDomain(request);

        assertEquals("Teclado", product.getCommercialName());
        assertEquals(brandId, product.getBrandId());
        assertEquals("donado", product.getProductType());
        assertNull(product.getStatus());
    }

    @Test
    void toResponseWithoutBrandNameLeavesBrandNameNull() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .id(id)
                .organizationId("org-1")
                .commercialName("Laptop HP")
                .brandId(UUID.randomUUID())
                .productType("venta")
                .status('A')
                .createdAt(now)
                .updatedAt(now)
                .build();

        ProductResponse response = mapper.toResponse(product);

        assertEquals(id, response.getId());
        assertEquals("org-1", response.getOrganizationId());
        assertEquals("Laptop HP", response.getCommercialName());
        assertEquals("venta", response.getProductType());
        assertEquals('A', response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertNull(response.getBrandName());
    }

    @Test
    void toResponseWithBrandNameSetsBrandName() {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .commercialName("Laptop HP")
                .build();

        ProductResponse response = mapper.toResponse(product, "Samsung");

        assertEquals("Samsung", response.getBrandName());
        assertEquals("Laptop HP", response.getCommercialName());
    }
}
