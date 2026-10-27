package pe.edu.vallegrande.sigrc.product.application.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateBrandRequest {
    private String name;

    public CreateBrandRequest(String name) {
        this.name = name;
    }
}
