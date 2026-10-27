package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateBrandRestRequest {

    @Size(max = 100, message = "El nombre de la marca no puede exceder 100 caracteres")
    private String name;

    public UpdateBrandRestRequest(String name) {
        this.name = name;
    }
}
