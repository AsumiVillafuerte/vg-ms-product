package pe.edu.vallegrande.sigrc.product.application.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ProductMapper {

    public Product toDomain(CreateProductRequest request) {
        return Product.builder()
                .commercialName(request.getCommercialName())
                .brandId(request.getBrandId())
                .productType(request.getProductType() != null ? request.getProductType() : "venta")
                .status('A')
                .createdAt(LocalDateTime.now(ZoneId.of("America/Lima")))
                .updatedAt(LocalDateTime.now(ZoneId.of("America/Lima")))
                .build();
    }

    public Product mapUpdateToDomain(UpdateProductRequest request) {
        return Product.builder()
                .commercialName(request.getCommercialName())
                .brandId(request.getBrandId())
                .productType(request.getProductType())
                .build();
    }

    public ProductResponse toResponse(Product product) {
        return toResponse(product, null);
    }

    public ProductResponse toResponse(Product product, String brandName) {
        return ProductResponse.builder()
                .id(product.getId())
                .organizationId(product.getOrganizationId())
                .commercialName(product.getCommercialName())
                .brandId(product.getBrandId())
                .brandName(brandName)
                .productType(product.getProductType())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
