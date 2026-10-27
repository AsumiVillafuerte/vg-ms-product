package pe.edu.vallegrande.sigrc.product.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.ConflictException;
import pe.edu.vallegrande.sigrc.product.domain.exception.DomainException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CreateBrandUseCaseImpl implements ICreateBrandUseCase {

    private final IBrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final ICurrentUserProvider currentUserProvider;

    public CreateBrandUseCaseImpl(IBrandRepository brandRepository,
                                  BrandMapper brandMapper,
                                  ICurrentUserProvider currentUserProvider) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Mono<BrandResponse> createBrand(CreateBrandRequest request) {
        log.debug("Creando marca: {}", request);
        Brand brand = brandMapper.toDomain(request);
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> {
                    brand.setOrganizationId(organizationId);
                    return ensureNameNotDuplicate(brand)
                            .then(Mono.defer(() -> brandRepository.save(brand)));
                })
                .doOnNext(saved -> log.debug("Marca creada con ID: {}", saved.getId()))
                .map(brandMapper::toResponse);
    }

    private Mono<Void> ensureNameNotDuplicate(Brand brand) {
        if (brand.getName() == null || brand.getName().isBlank()) {
            return Mono.error(new DomainException("El nombre de la marca no puede estar en blanco"));
        }
        String normalizedName = brand.getName().trim();
        brand.setName(normalizedName);
        return brandRepository.existsByName(normalizedName, brand.getOrganizationId())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new ConflictException(
                                "Ya existe una marca con el nombre '" + normalizedName + "' en su organizacion"));
                    }
                    return Mono.empty();
                });
    }
}
