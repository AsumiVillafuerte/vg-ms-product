package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CreateProductRestRequest {

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(max = 150, message = "El nombre comercial no puede exceder 150 caracteres")
    private String commercialName;

    @NotNull(message = "El ID de la marca es obligatorio")
    private UUID brandId;

    @Pattern(regexp = "venta|donado", message = "El tipo de producto debe ser 'venta' o 'donado'")
    private String productType = "venta";

    public CreateProductRestRequest(String commercialName, UUID brandId, String productType) {
        this.commercialName = commercialName;
        this.brandId = brandId;
        this.productType = productType;
    }
}
