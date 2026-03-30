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

    @Query("SELECT m FROM MovimientoStock m WHERE m.ingrediente.id IN :ingredienteIds " +
            "AND m.fecha >= :fechaDesde AND m.fecha <= :fechaHasta ORDER BY m.fecha ASC")
    List<MovimientoStock> findByIngredientesAndFechaBetween(
            @Param("ingredienteIds") List<Long> ingredienteIds,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta);

    List<MovimientoStock> findByIngredienteIdOrderByFechaDesc(Long ingredienteId);
}
