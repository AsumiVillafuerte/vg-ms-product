package pe.edu.vallegrande.sigrc.product.domain.port.out;

import pe.edu.vallegrande.sigrc.product.domain.model.Brand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IBrandRepository {
    Mono<Brand> save(Brand brand);
    Mono<Brand> findById(UUID id, String organizationId);
    Flux<Brand> findAll(String organizationId);
    Flux<Brand> findByStatus(Character status, String organizationId);
    Mono<Brand> update(Brand brand);

    /**
     * Verifica si existe una marca con el mismo nombre (sin distinguir mayusculas
     * ni espacios exteriores) dentro de la organizacion, sin excluir ninguna marca.
     */
    Mono<Boolean> existsByName(String name, String organizationId);

    /**
     * Verifica si existe una marca con el mismo nombre (sin distinguir mayusculas
     * ni espacios exteriores) dentro de la organizacion, excluyendo la marca dada.
     * Se usa al actualizar para permitir conservar el propio nombre.
     */
    Mono<Boolean> existsByName(String name, String organizationId, UUID excludeId);
}
