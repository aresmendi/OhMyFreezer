package com.ares.backend.repository;

import com.ares.backend.entity.TpvSkuMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad TpvSkuMapping.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface TpvSkuMappingRepository extends JpaRepository<TpvSkuMapping, Long> {

    /**
     * Resuelve el mapeo de un SKU de TPV dentro de un negocio concreto.
     * Devuelve la fila aunque esté pendiente ({@code receta == null}): es
     * el mismo registro que ve el jefe de cocina en la pantalla de mapeo
     * (D-B del diseño), no un estado separado.
     *
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @param skuTpv    Código de producto del TPV
     * @return Optional con el mapeo (mapeado o pendiente) si existe
     */
    Optional<TpvSkuMapping> findByNegocioIdAndSkuTpv(Long negocioId, String skuTpv);

    /**
     * Lista todos los mapeos de un negocio, más recientes primero.
     * Incluye tanto los mapeados como los pendientes.
     *
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Lista de mapeos ordenada por fechaCreacion DESC
     */
    List<TpvSkuMapping> findByNegocioIdOrderByFechaCreacionDesc(Long negocioId);

    /**
     * Busca un mapeo por ID, scoped al negocio del caller.
     * Un ID de otro negocio devuelve Optional.empty() — el caller no puede
     * distinguirlo de un ID inexistente (IDOR check implícito).
     *
     * @param id        ID del mapeo
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Optional con el mapeo si existe y pertenece al negocio
     */
    Optional<TpvSkuMapping> findByIdAndNegocioId(Long id, Long negocioId);
}
