package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.springframework.stereotype.Service;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IRestoreBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class RestoreBrandUseCaseImpl implements IRestoreBrandUseCase {

    private final IBrandRepository brandRepository;
    private final ICurrentUserProvider currentUserProvider;

    public RestoreBrandUseCaseImpl(IBrandRepository brandRepository,
                                   ICurrentUserProvider currentUserProvider) {
        this.brandRepository = brandRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Mono<Void> restoreBrand(UUID id) {
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> brandRepository.findById(id, organizationId))
                .switchIfEmpty(Mono.error(new NotFoundException("Marca no encontrada con ID: " + id)))
                .flatMap(brand -> {
                    brand.setStatus('A');
                    return brandRepository.update(brand);
                })
                .then();
    }
}
