package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IUpdateBrandUseCase {
    Mono<BrandResponse> updateBrand(UUID id, UpdateBrandRequest request);
}
