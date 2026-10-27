package pe.edu.vallegrande.sigrc.product.application.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateBrandRequest {
    private String name;

    public UpdateBrandRequest(String name) {
        this.name = name;
    }
}
