package com.ares.backend.repository;

import com.ares.backend.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /**
     * Busca movimientos de una lista de ingredientes en un rango de fechas,
     * scoped al negocio (tenant) del caller. Reemplaza a la variante sin
     * negocioId: los ingredienteIds llegan directamente del request del
     * cliente (query param), así que sin este scoping un caller podía pasar
     * ids de ingredientes de OTRO negocio y ver sus movimientos de stock.
     */
    @Query("SELECT m FROM MovimientoStock m WHERE m.ingrediente.id IN :ingredienteIds " +
            "AND m.fecha >= :fechaDesde AND m.fecha <= :fechaHasta " +
            "AND m.negocio.id = :negocioId ORDER BY m.fecha ASC")
    List<MovimientoStock> findByIngredientesAndFechaBetweenAndNegocioId(
            @Param("ingredienteIds") List<Long> ingredienteIds,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("negocioId") Long negocioId);

    List<MovimientoStock> findByIngredienteIdOrderByFechaDesc(Long ingredienteId);

    /**
     * Busca movimientos de un ingrediente específico, scoped al negocio
     * (tenant) del caller. MovimientoStock no puede apoyarse en la
     * asociación con Ingrediente para el filtrado (usuarioId es un Long
     * crudo sin FK, ver diseño), por lo que tiene su propia columna
     * negocio_id que se comprueba aquí explícitamente.
     *
     * @param ingredienteId ID del ingrediente
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Lista de movimientos de ese ingrediente en ese negocio
     */
    List<MovimientoStock> findByIngredienteIdAndNegocioId(Long ingredienteId, Long negocioId);
}
