package pe.edu.vallegrande.sigrc.product.domain.port.in;

import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import reactor.core.publisher.Mono;

public interface ICreateBrandUseCase {
    Mono<BrandResponse> createBrand(CreateBrandRequest request);
}
