package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateProductRestRequest {

    @Size(max = 150, message = "El nombre comercial no puede exceder 150 caracteres")
    private String commercialName;

    private UUID brandId;

    @Pattern(regexp = "venta|donado", message = "El tipo de producto debe ser 'venta' o 'donado'")
    private String productType;

    public UpdateProductRestRequest(String commercialName, UUID brandId, String productType) {
        this.commercialName = commercialName;
        this.brandId = brandId;
        this.productType = productType;
    }
}
