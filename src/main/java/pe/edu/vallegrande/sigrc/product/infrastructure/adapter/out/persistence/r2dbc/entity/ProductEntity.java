package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@Table("products")
public class ProductEntity {
    
    @Id
    private UUID id;
    
    @Column("organization_id")
    private String organizationId;
    
    @Column("commercial_name")
    private String commercialName;
    
    @Column("brand_id")
    private UUID brandId;
    
    @Column("product_type")
    private String productType;
    
    @Column("status")
    private String status;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;

    public ProductEntity(UUID id, String organizationId, String commercialName, UUID brandId,
                         String productType, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
