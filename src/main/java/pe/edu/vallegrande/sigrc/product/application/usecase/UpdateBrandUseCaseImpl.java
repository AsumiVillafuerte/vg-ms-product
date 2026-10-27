package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.ConflictException;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class UpdateBrandUseCaseImpl implements IUpdateBrandUseCase {

    private final IBrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final ICurrentUserProvider currentUserProvider;

    public UpdateBrandUseCaseImpl(IBrandRepository brandRepository,
                                  BrandMapper brandMapper,
                                  ICurrentUserProvider currentUserProvider) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Mono<BrandResponse> updateBrand(UUID id, UpdateBrandRequest request) {
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> brandRepository.findById(id, organizationId))
                .switchIfEmpty(Mono.error(new NotFoundException("Marca no encontrada con ID: " + id)))
                .flatMap(existing -> {
                    Brand updated = brandMapper.mapUpdateToDomain(request);
                    updated.setId(existing.getId());
                    updated.setOrganizationId(existing.getOrganizationId());
                    updated.setStatus(existing.getStatus());
                    if (updated.getName() == null || updated.getName().isBlank()) {
                        updated.setName(existing.getName());
                    } else {
                        updated.setName(updated.getName().trim());
                    }
                    return ensureNotDuplicate(existing, updated).thenReturn(updated);
                })
                .flatMap(brandRepository::update)
                .map(brandMapper::toResponse);
    }

    private Mono<Void> ensureNotDuplicate(Brand existing, Brand updated) {
        if (updated.getName().equalsIgnoreCase(existing.getName())) {
            return Mono.empty();
        }
        String normalizedName = updated.getName();
        return brandRepository.existsByName(normalizedName,
                        updated.getOrganizationId(), updated.getId())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new ConflictException(
                                "Ya existe una marca con el nombre '" + normalizedName + "' en su organizacion"));
                    }
                    return Mono.empty();
                });
    }
}
