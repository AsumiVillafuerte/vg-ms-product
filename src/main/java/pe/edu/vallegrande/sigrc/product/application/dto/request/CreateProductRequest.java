package pe.edu.vallegrande.sigrc.product.application.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CreateProductRequest {
    private String commercialName;
    private UUID brandId;
    private String productType;

    public CreateProductRequest(String commercialName, UUID brandId, String productType) {
        this.commercialName = commercialName;
        this.brandId = brandId;
        this.productType = productType;
    }
}
