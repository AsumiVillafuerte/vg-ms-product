package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateProductRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.ProductRestResponse;

@Component
public class ProductRestMapper {

    public CreateProductRequest toApplication(CreateProductRestRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateProductRequest(request.getCommercialName(), request.getBrandId(), request.getProductType());
    }

    public UpdateProductRequest toApplication(UpdateProductRestRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateProductRequest(request.getCommercialName(), request.getBrandId(), request.getProductType());
    }

    public ProductRestResponse toRest(ProductResponse response) {
        if (response == null) {
            return null;
        }
        return ProductRestResponse.builder()
                .id(response.getId())
                .organizationId(response.getOrganizationId())
                .commercialName(response.getCommercialName())
                .brandId(response.getBrandId())
                .brandName(response.getBrandName())
                .productType(response.getProductType())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
