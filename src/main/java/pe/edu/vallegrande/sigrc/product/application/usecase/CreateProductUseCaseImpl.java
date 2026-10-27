package pe.edu.vallegrande.sigrc.product.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateProductRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Product;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
public class CreateProductUseCaseImpl implements ICreateProductUseCase {

    private final IProductRepository productRepository;
    private final IBrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ICurrentUserProvider currentUserProvider;

    public CreateProductUseCaseImpl(IProductRepository productRepository,
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
    public Mono<ProductResponse> createProduct(CreateProductRequest request) {
        log.debug("Creando producto: {}", request);
        Product product = productMapper.toDomain(request);
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> {
                    product.setOrganizationId(organizationId);
                    return validateBrand(product.getBrandId(), organizationId)
                            .then(Mono.defer(() -> productRepository.save(product)))
                            .doOnNext(saved -> log.debug("Producto creado con ID: {}", saved.getId()))
                            .flatMap(saved -> getBrandName(saved.getBrandId(), organizationId)
                                    .map(brandName -> productMapper.toResponse(saved, brandName)));
                });
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
