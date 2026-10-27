package pe.edu.vallegrande.sigrc.product.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.product.application.dto.response.ProductResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.ProductMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
public class GetProductUseCaseImpl implements IGetProductUseCase {

    private final IProductRepository productRepository;
    private final IBrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ICurrentUserProvider currentUserProvider;

    public GetProductUseCaseImpl(IProductRepository productRepository,
                                 IBrandRepository brandRepository,
                                 ProductMapper productMapper,
                                 ICurrentUserProvider currentUserProvider) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.productMapper = productMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Mono<ProductResponse> getProductById(UUID id) {
        log.debug("Buscando producto con ID: {}", id);
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> productRepository.findById(id, organizationId)
                        .doOnNext(product -> log.debug("Producto encontrado: {}", product))
                        .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado con ID: " + id)))
                        .flatMap(product -> getBrandName(product.getBrandId(), organizationId)
                                .map(brandName -> productMapper.toResponse(product, brandName))));
    }

    @Override
    public Flux<ProductResponse> getAllProducts() {
        log.debug("Listando todos los productos (activos e inactivos)");
        return currentUserProvider.getOrganizationId()
                .flatMapMany(organizationId -> productRepository.findAll(organizationId)
                        .doOnNext(product -> log.debug("Producto: {}", product))
                        .flatMap(product -> getBrandName(product.getBrandId(), organizationId)
                                .map(brandName -> productMapper.toResponse(product, brandName))))
                .doOnComplete(() -> log.debug("Listado de productos completado"));
    }

    @Override
    public Flux<ProductResponse> getProductsByStatus(Character status) {
        log.debug("Listando productos con estado: {}", status);
        return currentUserProvider.getOrganizationId()
                .flatMapMany(organizationId -> productRepository.findByStatus(status, organizationId)
                        .doOnNext(product -> log.debug("Producto con estado {}: {}", status, product))
                        .flatMap(product -> getBrandName(product.getBrandId(), organizationId)
                                .map(brandName -> productMapper.toResponse(product, brandName))))
                .doOnComplete(() -> log.debug("Listado de productos por estado completado"));
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
