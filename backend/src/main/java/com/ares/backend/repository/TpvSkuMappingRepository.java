package com.ares.backend.repository;

import com.ares.backend.entity.TpvSkuMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
