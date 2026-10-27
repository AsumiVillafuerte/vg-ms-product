package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IDeleteProductUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IProductRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class DeleteProductUseCaseImpl implements IDeleteProductUseCase {

    private final IProductRepository productRepository;
    private final ICurrentUserProvider currentUserProvider;

    public DeleteProductUseCaseImpl(IProductRepository productRepository,
                                    ICurrentUserProvider currentUserProvider) {
        this.productRepository = productRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public Mono<Void> logicalDeleteProduct(UUID id) {
        return currentUserProvider.getOrganizationId()
                .flatMap(organizationId -> productRepository.findById(id, organizationId))
                .switchIfEmpty(Mono.error(new NotFoundException("Producto no encontrado con ID: " + id)))
                .flatMap(product -> {
                    product.setStatus('I');
                    product.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Lima")));
                    return productRepository.update(product);
                })
                .then();
    }
}
