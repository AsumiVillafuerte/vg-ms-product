package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IGetBrandUseCase {
    Mono<BrandResponse> getBrandById(UUID id);
    Flux<BrandResponse> getAllBrands();
    Flux<BrandResponse> getBrandsByStatus(Character status);
}
