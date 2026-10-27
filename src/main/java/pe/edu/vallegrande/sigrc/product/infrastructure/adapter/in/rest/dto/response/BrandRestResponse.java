package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
public class BrandRestResponse {
    private UUID id;
    private String organizationId;
    private String name;
    private Character status;

    public BrandRestResponse(UUID id, String organizationId, String name, Character status) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.status = status;
    }
}
