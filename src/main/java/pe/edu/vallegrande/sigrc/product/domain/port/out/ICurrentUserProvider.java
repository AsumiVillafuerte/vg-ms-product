package pe.edu.vallegrande.sigrc.product.domain.port.out;

import reactor.core.publisher.Mono;

public interface ICurrentUserProvider {
    Mono<String> getOrganizationId();
    Mono<String> getUserId();
}
