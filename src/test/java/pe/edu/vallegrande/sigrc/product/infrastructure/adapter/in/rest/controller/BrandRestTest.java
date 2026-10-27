package pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.request.CreateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.request.UpdateBrandRequest;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.domain.port.in.ICreateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IDeleteBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IGetBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IRestoreBrandUseCase;
import pe.edu.vallegrande.sigrc.product.domain.port.in.IUpdateBrandUseCase;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.CreateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.dto.request.UpdateBrandRestRequest;
import pe.edu.vallegrande.sigrc.product.infrastructure.adapter.in.rest.mapper.BrandRestMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandRestTest {

    @Mock
    private ICreateBrandUseCase createBrandUseCase;
    @Mock
    private IGetBrandUseCase getBrandUseCase;
    @Mock
    private IUpdateBrandUseCase updateBrandUseCase;
    @Mock
    private IDeleteBrandUseCase deleteBrandUseCase;
    @Mock
    private IRestoreBrandUseCase restoreBrandUseCase;

    private BrandRestMapper restMapper;
    private BrandRest controller;

    private UUID brandId;
    private BrandResponse brandResponse;

    @BeforeEach
    void setUp() {
        restMapper = new BrandRestMapper();
        controller = new BrandRest(createBrandUseCase, getBrandUseCase, updateBrandUseCase,
                deleteBrandUseCase, restoreBrandUseCase, restMapper);
        brandId = UUID.randomUUID();
        brandResponse = BrandResponse.builder()
                .id(brandId)
                .organizationId("org-1")
                .name("Samsung")
                .status('A')
                .build();
    }

    @Test
    void createBrandWrapsResponseWith201() {
        CreateBrandRestRequest request = new CreateBrandRestRequest("Samsung");
        when(createBrandUseCase.createBrand(new CreateBrandRequest("Samsung"))).thenReturn(Mono.just(brandResponse));

        StepVerifier.create(controller.createBrand(request))
                .assertNext(response -> {
                    assertEquals(201, response.getStatus());
                    assertEquals("Marca creada exitosamente", response.getMessage());
                    assertEquals(restMapper.toRest(brandResponse), response.getData());
                })
                .verifyComplete();
    }

    @Test
    void getBrandByIdWrapsResponseWith200() {
        when(getBrandUseCase.getBrandById(brandId)).thenReturn(Mono.just(brandResponse));

        StepVerifier.create(controller.getBrandById(brandId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Marca encontrada", response.getMessage());
                })
                .verifyComplete();

        verify(getBrandUseCase).getBrandById(brandId);
    }

    @Test
    void getAllBrandsCollectsList() {
        when(getBrandUseCase.getAllBrands()).thenReturn(Flux.just(brandResponse));

        StepVerifier.create(controller.getAllBrands())
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Lista de marcas", response.getMessage());
                    assertEquals(1, response.getData().size());
                })
                .verifyComplete();
    }

    @Test
    void getBrandsByStatusBuildsMessageWithStatus() {
        when(getBrandUseCase.getBrandsByStatus('I')).thenReturn(Flux.just(brandResponse));

        StepVerifier.create(controller.getBrandsByStatus('I'))
                .assertNext(response -> {
                    assertEquals("Marcas filtradas por estado: I", response.getMessage());
                    assertEquals(1, response.getData().size());
                })
                .verifyComplete();
    }

    @Test
    void updateBrandWrapsResponseWith200() {
        UpdateBrandRestRequest request = new UpdateBrandRestRequest("LG");
        when(updateBrandUseCase.updateBrand(brandId, new UpdateBrandRequest("LG"))).thenReturn(Mono.just(brandResponse));

        StepVerifier.create(controller.updateBrand(brandId, request))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Marca actualizada exitosamente", response.getMessage());
                })
                .verifyComplete();
    }

    @Test
    void deactivateBrandReturnsOkWithNullData() {
        when(deleteBrandUseCase.logicalDeleteBrand(brandId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.deactivateBrand(brandId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Marca desactivada exitosamente", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        verify(deleteBrandUseCase).logicalDeleteBrand(brandId);
    }

    @Test
    void activateBrandReturnsOkWithNullData() {
        when(restoreBrandUseCase.restoreBrand(brandId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.activateBrand(brandId))
                .assertNext(response -> {
                    assertEquals(200, response.getStatus());
                    assertEquals("Marca reactivada exitosamente", response.getMessage());
                    assertNull(response.getData());
                })
                .verifyComplete();

        verify(restoreBrandUseCase).restoreBrand(brandId);
    }
}
