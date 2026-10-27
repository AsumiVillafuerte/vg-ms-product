package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.ProductEntity;

@Component
public class ProductPersistenceMapper {

    public Product toDomain(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .commercialName(entity.getCommercialName())
                .brandId(entity.getBrandId())
                .productType(entity.getProductType())
                .status(entity.getStatus() != null ? entity.getStatus().charAt(0) : 'A')
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ProductEntity toEntity(Product domain) {
        return ProductEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .commercialName(domain.getCommercialName())
                .brandId(domain.getBrandId())
                .productType(domain.getProductType())
                .status(domain.getStatus() != null ? String.valueOf(domain.getStatus()) : "A")
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
