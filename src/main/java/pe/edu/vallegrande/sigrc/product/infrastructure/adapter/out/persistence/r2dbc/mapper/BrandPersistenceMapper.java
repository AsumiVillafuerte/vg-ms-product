package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity.BrandEntity;

@Component
public class BrandPersistenceMapper {

    public Brand toDomain(BrandEntity entity) {
        return Brand.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .name(entity.getName())
                .status(entity.getStatus() != null ? entity.getStatus().charAt(0) : 'A')
                .build();
    }

    public BrandEntity toEntity(Brand domain) {
        return BrandEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .name(domain.getName())
                .status(domain.getStatus() != null ? String.valueOf(domain.getStatus()) : "A")
                .build();
    }
}
