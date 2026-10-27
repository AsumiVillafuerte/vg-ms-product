package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.out.persistence.memory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("mock")
public class MockDataStore {

    private final Map<UUID, Brand> brands = new ConcurrentHashMap<>();
    private final Map<UUID, Product> products = new ConcurrentHashMap<>();

    public MockDataStore(@Value("${app.security.default-organization-id:default}") String organizationId) {
        seedBrands(organizationId);
        seedProducts(organizationId);
    }

    public Map<UUID, Brand> brands() {
        return brands;
    }

    public Map<UUID, Product> products() {
        return products;
    }

    private void seedBrands(String organizationId) {
        String[] names = {"Valle Grande", "Andina", "Costa Azul", "El Sol", "Natura"};
        for (int i = 0; i < names.length; i++) {
            UUID id = deterministicId("brand-" + names[i]);
            brands.put(id, Brand.builder()
                    .id(id)
                    .organizationId(organizationId)
                    .name(names[i])
                    .status('A')
                    .build());
        }
    }

    private void seedProducts(String organizationId) {
        UUID[] brandIds = brands.keySet().toArray(UUID[]::new);
        String[] types = {"ALIMENTO", "BEBIDA", "LIMPIEZA", "CUIDADO_PERSONAL", "HOGAR"};
        LocalDateTime base = LocalDateTime.now();
        for (int i = 0; i < 20; i++) {
            UUID id = deterministicId("product-" + i);
            products.put(id, Product.builder()
                    .id(id)
                    .organizationId(organizationId)
                    .commercialName(String.format("Producto Mock %02d", i + 1))
                    .brandId(brandIds[i % brandIds.length])
                    .productType(types[i % types.length])
                    .status(i % 4 == 0 ? 'I' : 'A')
                    .createdAt(base.minusDays(i))
                    .updatedAt(base.minusDays(i))
                    .build());
        }
    }

    private UUID deterministicId(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
