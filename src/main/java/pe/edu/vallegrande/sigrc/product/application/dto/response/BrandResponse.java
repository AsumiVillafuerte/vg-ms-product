package pe.edu.vallegrande.sigrc.product.application.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
public class BrandResponse {
    private UUID id;
    private String organizationId;
    private String name;
    private Character status;

    public BrandResponse(UUID id, String organizationId, String name, Character status) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.status = status;
    }
}
