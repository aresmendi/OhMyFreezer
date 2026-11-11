package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.TpvSkuMapping;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.RecetaRepository;
import com.ares.backend.repository.TpvSkuMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio CRUD para los mapeos SKU del TPV: lista los mapeos del negocio,
 * asigna una receta a un mapeo pendiente y desmapea (pone receta a null)
 * un mapeo ya resuelto. Todos los accesos están scoped al negocio del
 * caller autenticado ({@link SecurityUtils#getNegocioId()}); el caller
 * nunca puede ver ni modificar mapeos de otro negocio (IDOR check implícito
 * en {@code findByIdAndNegocioId}).
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TpvSkuMappingService {

    private final TpvSkuMappingRepository tpvSkuMappingRepository;
    private final RecetaRepository recetaRepository;

    /**
     * Lista todos los mapeos SKU del negocio del caller, más recientes primero.
     * Incluye tanto los mapeados (con receta) como los pendientes (sin receta).
     *
     * @return Lista de mapeos del negocio, ordenada por fechaCreacion DESC
     */
    @Transactional(readOnly = true)
    public List<TpvSkuMapping> listar() {
        return tpvSkuMappingRepository.findByNegocioIdOrderByFechaCreacionDesc(
                SecurityUtils.getNegocioId());
    }

    /**
     * Asigna una receta a un mapeo SKU del negocio del caller.
     * Verifica que tanto el mapeo como la receta pertenezcan al negocio del
     * caller (IDOR check): un ID de otro negocio es indistinguible de un ID
     * inexistente y devuelve 404.
     *
     * @param mappingId ID del mapeo SKU a actualizar
     * @param recetaId  ID de la receta a asignar
     * @return Mapeo actualizado con la receta asignada
     * @throws RecursoNoEncontradoException Si el mapeo o la receta no existen
     *                                      o pertenecen a otro negocio
     */
    @Transactional
    public TpvSkuMapping asignarReceta(Long mappingId, Long recetaId) {
        Long negocioId = SecurityUtils.getNegocioId();

        TpvSkuMapping mapping = tpvSkuMappingRepository.findByIdAndNegocioId(mappingId, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mapeo SKU no encontrado"));

        Receta receta = recetaRepository.findByIdAndNegocioId(recetaId, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receta no encontrada"));

        mapping.setReceta(receta);
        mapping.setFechaActualizacion(LocalDateTime.now());

        return tpvSkuMappingRepository.save(mapping);
    }

    /**
     * Elimina la asignación de receta de un mapeo SKU (lo vuelve a dejar
     * pendiente). Mismo IDOR check que {@link #asignarReceta}.
     *
     * @param mappingId ID del mapeo SKU a desmapear
     * @return Mapeo actualizado con receta a null
     * @throws RecursoNoEncontradoException Si el mapeo no existe o pertenece
     *                                      a otro negocio
     */
    @Transactional
    public TpvSkuMapping desmapear(Long mappingId) {
        Long negocioId = SecurityUtils.getNegocioId();

        TpvSkuMapping mapping = tpvSkuMappingRepository.findByIdAndNegocioId(mappingId, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mapeo SKU no encontrado"));

        mapping.setReceta(null);
        mapping.setFechaActualizacion(LocalDateTime.now());

        return tpvSkuMappingRepository.save(mapping);
    }
}
