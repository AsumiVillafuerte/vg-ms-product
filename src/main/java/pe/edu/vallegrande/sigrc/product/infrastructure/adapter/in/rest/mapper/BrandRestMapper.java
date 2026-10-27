package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.BrandRestResponse;

@Component
public class BrandRestMapper {

    public CreateBrandRequest toApplication(CreateBrandRestRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateBrandRequest(request.getName());
    }

    public UpdateBrandRequest toApplication(UpdateBrandRestRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateBrandRequest(request.getName());
    }

    public BrandRestResponse toRest(BrandResponse response) {
        if (response == null) {
            return null;
        }
        return BrandRestResponse.builder()
                .id(response.getId())
                .organizationId(response.getOrganizationId())
                .name(response.getName())
                .status(response.getStatus())
                .build();
    }
}
