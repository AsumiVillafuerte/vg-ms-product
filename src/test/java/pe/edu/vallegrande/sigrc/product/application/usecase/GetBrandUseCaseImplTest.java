package pe.edu.vallegrande.sigrc.product.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.sigrc.product.application.dto.response.BrandResponse;
import pe.edu.vallegrande.sigrc.product.application.mapper.BrandMapper;
import pe.edu.vallegrande.sigrc.product.domain.exception.NotFoundException;
import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import pe.edu.vallegrande.sigrc.product.domain.port.out.IBrandRepository;
import pe.edu.vallegrande.sigrc.product.domain.port.out.ICurrentUserProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para 
 * Valida la consulta de marcas por ID, listado completo
 * y filtrado por estado.
 */
@ExtendWith(MockitoExtension.class)
class GetBrandUseCaseImplTest {

    @Mock
    private IBrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private ICurrentUserProvider currentUserProvider;

    @InjectMocks
    private GetBrandUseCaseImpl getBrandUseCase;

    private UUID brandId;
    private Brand brand;
    private BrandResponse brandResponse;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
        brand = Brand.builder()
                .id(brandId).name("Samsung").status('A').build();
        brandResponse = BrandResponse.builder()
                .id(brandId).name("Samsung").status('A').build();
        when(currentUserProvider.getOrganizationId()).thenReturn(Mono.just("org-1"));
    }

    /**
     * Caso positivo: consulta una marca por ID.
     * Verifica que se retorne la respuesta con los datos mapeados.
     */
    @Test
    void shouldGetBrandByIdSuccessfully() {
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.just(brand));
        when(brandMapper.toResponse(brand)).thenReturn(brandResponse);

        StepVerifier.create(getBrandUseCase.getBrandById(brandId))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(brandId, response.getId());
                    assertEquals("Samsung", response.getName());
                    assertEquals('A', response.getStatus());
                })
                .verifyComplete();
    }

    /**
     * Caso negativo: no existe una marca con el ID consultado.
     * Verifica que se emita un {@link NotFoundException}.
     */
    @Test
    void shouldReturnNotFoundWhenBrandDoesNotExist() {
        when(brandRepository.findById(brandId, "org-1")).thenReturn(Mono.empty());

        StepVerifier.create(getBrandUseCase.getBrandById(brandId))
                .expectErrorMatches(error -> error instanceof NotFoundException
                        && error.getMessage().contains("Marca no encontrada"))
                .verify();
    }

    /**
     * Caso positivo: lista todas las marcas.
     * Verifica que se emitan todas las respuestas en orden.
     */
    @Test
    void shouldListAllBrands() {
        Brand secondBrand = Brand.builder()
                .id(UUID.randomUUID()).name("Apple").status('I').build();
        BrandResponse secondResponse = BrandResponse.builder()
                .id(secondBrand.getId()).name("Apple").status('I').build();

        when(brandRepository.findAll("org-1")).thenReturn(Flux.just(brand, secondBrand));
        when(brandMapper.toResponse(brand)).thenReturn(brandResponse);
        when(brandMapper.toResponse(secondBrand)).thenReturn(secondResponse);

        StepVerifier.create(getBrandUseCase.getAllBrands())
                .expectNext(brandResponse, secondResponse)
                .verifyComplete();
    }

    /**
     * Caso positivo: lista marcas filtrando por estado.
     * Verifica que la consulta use el estado solicitado.
     */
    @Test
    void shouldListBrandsByStatus() {
        when(brandRepository.findByStatus('I', "org-1")).thenReturn(Flux.just(brand));
        when(brandMapper.toResponse(brand)).thenReturn(brandResponse);

        StepVerifier.create(getBrandUseCase.getBrandsByStatus('I'))
                .expectNext(brandResponse)
                .verifyComplete();

        verify(brandRepository).findByStatus('I', "org-1");
    }
}