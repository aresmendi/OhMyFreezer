package com.ares.backend.repository;

import com.ares.backend.entity.VentaTpvLinea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link VentaTpvLinea}: snapshot de stock
 * descontado por una venta TPV.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface VentaTpvLineaRepository extends JpaRepository<VentaTpvLinea, Long> {

    /**
     * Devuelve todas las líneas de snapshot de una venta TPV concreta,
     * ordenadas por su ID (orden de inserción).
     *
     * @param ventaTpvId ID de la venta TPV
     * @return lista de líneas (vacía si la venta no descontó stock)
     */
    List<VentaTpvLinea> findByVentaTpvId(Long ventaTpvId);
}
