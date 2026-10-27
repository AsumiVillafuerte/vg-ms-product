package pe.edu.vallegrande.sigrc.product.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
public class GetBrandUseCaseImpl implements IGetBrandUseCase {

    private final IBrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final ICurrentUserProvider currentUserProvider;

    public GetBrandUseCaseImpl(IBrandRepository brandRepository,
                               BrandMapper brandMapper,
                               ICurrentUserProvider currentUserProvider) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Mono<BrandResponse> getBrandById(UUID id) {
        log.debug("Buscando marca con ID: {}", id);
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> brandRepository.findById(id, organizationId))
                .doOnNext(brand -> log.debug("Marca encontrada: {}", brand))
                .switchIfEmpty(Mono.error(new NotFoundException("Marca no encontrada con ID: " + id)))
                .map(brandMapper::toResponse);
    }

    @Override
    public Flux<BrandResponse> getAllBrands() {
        log.debug("Listando todas las marcas");
        return currentUserProvider.getOrganizationId()
                .flatMapMany(brandRepository::findAll)
                .doOnNext(brand -> log.debug("Marca: {}", brand))
                .map(brandMapper::toResponse)
                .doOnComplete(() -> log.debug("Listado de marcas completado"));
    }

    @Override
    public Flux<BrandResponse> getBrandsByStatus(Character status) {
        log.debug("Listando marcas con estado: {}", status);
        return currentUserProvider.getOrganizationId()
                .flatMapMany(organizationId -> brandRepository.findByStatus(status, organizationId))
                .doOnNext(brand -> log.debug("Marca con estado {}: {}", status, brand))
                .map(brandMapper::toResponse)
                .doOnComplete(() -> log.debug("Listado de marcas por estado completado"));
    }
}
