package pe.edu.vallegrande.sigrc.product.application.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;

@Component
public class BrandMapper {

    public Brand toDomain(CreateBrandRequest request) {
        return Brand.builder()
                .name(request.getName())
                .status('A')
                .build();
    }

    public Brand mapUpdateToDomain(UpdateBrandRequest request) {
        return Brand.builder()
                .name(request.getName())
                .build();
    }

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .organizationId(brand.getOrganizationId())
                .name(brand.getName())
                .status(brand.getStatus())
                .build();
    }
}
