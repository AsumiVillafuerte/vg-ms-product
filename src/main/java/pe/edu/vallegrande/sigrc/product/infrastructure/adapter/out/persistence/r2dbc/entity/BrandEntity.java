package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.r2dbc.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@Table("brands")
public class BrandEntity {

    @Id
    private UUID id;

    @Column("organization_id")
    private String organizationId;

    @Column("name")
    private String name;

    @Column("status")
    private String status;

    public BrandEntity(UUID id, String organizationId, String name, String status) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.status = status;
    }
}
