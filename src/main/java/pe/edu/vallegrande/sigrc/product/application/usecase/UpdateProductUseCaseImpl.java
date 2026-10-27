package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class UpdateProductUseCaseImpl implements IUpdateProductUseCase {

    private final IProductRepository productRepository;
    private final IBrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ICurrentUserProvider currentUserProvider;

    public UpdateProductUseCaseImpl(IProductRepository productRepository,
                                    IBrandRepository brandRepository,
                                    ProductMapper productMapper,
                                    ICurrentUserProvider currentUserProvider) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.productMapper = productMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public Mono<ProductResponse> updateProduct(UUID id, UpdateProductRequest request) {
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> productRepository.findById(id, organizationId)
                        .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado con ID: " + id)))
                        .flatMap(existing -> {
                            Product updated = productMapper.mapUpdateToDomain(request);
                            updated.setId(existing.getId());
                            updated.setOrganizationId(existing.getOrganizationId());
                            updated.setStatus(existing.getStatus());
                            updated.setCreatedAt(existing.getCreatedAt());
                            updated.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Lima")));
                            if (updated.getCommercialName() == null) updated.setCommercialName(existing.getCommercialName());
                            if (updated.getBrandId() == null) updated.setBrandId(existing.getBrandId());
                            if (updated.getProductType() == null) updated.setProductType(existing.getProductType());
                            return validateBrand(updated.getBrandId(), organizationId)
                                    .then(Mono.defer(() -> productRepository.update(updated)));
                        })
                        .flatMap(updated -> getBrandName(updated.getBrandId(), organizationId)
                                .map(brandName -> productMapper.toResponse(updated, brandName))));
    }

    private Mono<Void> validateBrand(UUID brandId, String organizationId) {
        if (brandId == null) {
            return Mono.empty();
        }
        return brandRepository.findById(brandId, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Marca no encontrada con ID: " + brandId + " en su organizacion")))
                .then();
    }

    private Mono<String> getBrandName(UUID brandId, String organizationId) {
        if (brandId == null) {
            return Mono.just("");
        }
        return brandRepository.findById(brandId, organizationId)
                .map(brand -> brand.getName())
                .onErrorResume(e -> Mono.just(""));
    }
}
