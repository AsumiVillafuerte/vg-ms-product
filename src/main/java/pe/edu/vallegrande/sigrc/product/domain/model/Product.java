package pe.edu.vallegrande.sigrc.product.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
public class Product {
    private UUID id;
    private String organizationId;
    private String commercialName;
    private UUID brandId;
    private String productType;
    private Character status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product(UUID id, String organizationId, String commercialName, UUID brandId,
                   String productType, Character status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.commercialName = commercialName;
        this.brandId = brandId;
        this.productType = productType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
