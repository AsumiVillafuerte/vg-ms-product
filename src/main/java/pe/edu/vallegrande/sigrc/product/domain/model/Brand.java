package pe.edu.vallegrande.sigrc.product.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
public class Brand {
    private UUID id;
    private String organizationId;
    private String name;
    private Character status;

    public Brand(UUID id, String organizationId, String name, Character status) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.status = status;
    }
}
