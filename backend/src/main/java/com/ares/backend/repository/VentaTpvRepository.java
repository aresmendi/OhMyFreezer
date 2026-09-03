package com.ares.backend.repository;

import com.ares.backend.entity.VentaTpv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad VentaTpv.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface VentaTpvRepository extends JpaRepository<VentaTpv, Long> {

    /**
     * Resuelve un evento (venta o anulación) por su propia identidad de
     * idempotencia dentro de un negocio. Usado por
     * {@code reservarIdempotencia()} (D-A del diseño): la constraint única
     * de base de datos ({@code uk_ventas_tpv_external}) es quien arbitra
     * el replay, pero este finder permite comprobarlo también desde
     * código de servicio.
     *
     * @param negocioId  ID del negocio (tenant) del caller autenticado
     * @param externalId Identidad de idempotencia propia del evento
     * @return Optional con el evento si ya fue registrado
     */
    Optional<VentaTpv> findByNegocioIdAndExternalId(Long negocioId, String externalId);

    /**
     * Resuelve la venta original referenciada por una anulación, dentro
     * de un negocio. Usado al procesar un evento ANULACION: si no hay
     * resultado, la anulación es huérfana (D del diseño); si ya hay uno
     * de tipo ANULACION previo con ese mismo {@code externalIdOriginal},
     * una segunda anulación viola {@code uk_ventas_tpv_original} y
     * responde DUPLICADA.
     *
     * @param negocioId          ID del negocio (tenant) del caller autenticado
     * @param externalIdOriginal externalId de la venta que se anula
     * @return Optional con el evento (venta original o anulación previa) si existe
     */
    Optional<VentaTpv> findByNegocioIdAndExternalIdOriginal(Long negocioId, String externalIdOriginal);
}
