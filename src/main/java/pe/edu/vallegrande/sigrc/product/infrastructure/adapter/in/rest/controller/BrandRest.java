package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IDeleteBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IRestoreBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.ApiResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.response.BrandRestResponse;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper.BrandRestMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandRest {

    private final ICreateBrandUseCase createBrandUseCase;
    private final IGetBrandUseCase getBrandUseCase;
    private final IUpdateBrandUseCase updateBrandUseCase;
    private final IDeleteBrandUseCase deleteBrandUseCase;
    private final IRestoreBrandUseCase restoreBrandUseCase;
    private final BrandRestMapper restMapper;

    public BrandRest(ICreateBrandUseCase createBrandUseCase,
                     IGetBrandUseCase getBrandUseCase,
                     IUpdateBrandUseCase updateBrandUseCase,
                     IDeleteBrandUseCase deleteBrandUseCase,
                     IRestoreBrandUseCase restoreBrandUseCase,
                     BrandRestMapper restMapper) {
        this.createBrandUseCase = createBrandUseCase;
        this.getBrandUseCase = getBrandUseCase;
        this.updateBrandUseCase = updateBrandUseCase;
        this.deleteBrandUseCase = deleteBrandUseCase;
        this.restoreBrandUseCase = restoreBrandUseCase;
        this.restMapper = restMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<BrandRestResponse>> createBrand(
            @Valid @RequestBody CreateBrandRestRequest request) {
        return createBrandUseCase.createBrand(restMapper.toApplication(request))
                .map(restMapper::toRest)
                .map(brand -> new ApiResponse<>(HttpStatus.CREATED.value(), "Marca creada exitosamente", brand));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<BrandRestResponse>> getBrandById(@PathVariable UUID id) {
        return getBrandUseCase.getBrandById(id)
                .map(restMapper::toRest)
                .map(brand -> new ApiResponse<>(HttpStatus.OK.value(), "Marca encontrada", brand));
    }

    @GetMapping
    public Mono<ApiResponse<List<BrandRestResponse>>> getAllBrands() {
        return getBrandUseCase.getAllBrands()
                .map(restMapper::toRest)
                .collectList()
                .map(brands -> new ApiResponse<>(HttpStatus.OK.value(), "Lista de marcas", brands));
    }

    @GetMapping("/status/{status}")
    public Mono<ApiResponse<List<BrandRestResponse>>> getBrandsByStatus(@PathVariable Character status) {
        return getBrandUseCase.getBrandsByStatus(status)
                .map(restMapper::toRest)
                .collectList()
                .map(brands -> new ApiResponse<>(HttpStatus.OK.value(), "Marcas filtradas por estado: " + status, brands));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<BrandRestResponse>> updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandRestRequest request) {
        return updateBrandUseCase.updateBrand(id, restMapper.toApplication(request))
                .map(restMapper::toRest)
                .map(brand -> new ApiResponse<>(HttpStatus.OK.value(), "Marca actualizada exitosamente", brand));
    }

    @PatchMapping("/{id}/deactivate")
    public Mono<ApiResponse<Void>> deactivateBrand(@PathVariable UUID id) {
        return deleteBrandUseCase.logicalDeleteBrand(id)
                .then(Mono.just(new ApiResponse<>(HttpStatus.OK.value(), "Marca desactivada exitosamente", null)));
    }

    @PatchMapping("/{id}/activate")
    public Mono<ApiResponse<Void>> activateBrand(@PathVariable UUID id) {
        return restoreBrandUseCase.restoreBrand(id)
                .then(Mono.just(new ApiResponse<>(HttpStatus.OK.value(), "Marca reactivada exitosamente", null)));
    }
}
