package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.MovimientoStockResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.MovimientoStock;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.MovimientoStockRepository;
import com.ares.backend.repository.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovimientoStockService {

    private final MovimientoStockRepository movimientoStockRepository;
    private final NegocioRepository negocioRepository;

    /**
     * Registra un movimiento de stock. A diferencia de las demás entidades
     * tenant-owned, MovimientoStock no puede derivar su negocio de una
     * asociación FK filtrable (usuarioId es un Long crudo) — por eso el
     * negocio del caller autenticado se resuelve y setea aquí explícitamente
     * en TODO movimiento creado, cerrando el hueco DEFAULT 1 de la
     * migración V2 para la tabla movimientos_stock.
     */
    @Transactional
    public void registrarMovimiento(
            com.ares.backend.entity.Ingrediente ingrediente,
            Double cantidadAnterior,
            Double cantidadNueva,
            String tipo,
            String motivo,
            Long usuarioId) {
        Negocio negocio = negocioRepository.findById(SecurityUtils.getNegocioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));

        MovimientoStock movimiento = new MovimientoStock(
                ingrediente, cantidadAnterior, cantidadNueva, tipo, motivo, usuarioId);
        movimiento.setNegocio(negocio);
        movimientoStockRepository.save(movimiento);
    }

    /**
     * Obtiene los movimientos de una lista de ingredientes en un rango de
     * fechas, scoped al negocio del caller. Los ingredienteIds llegan del
     * request del cliente: sin este scoping, un caller podía pedir ids de
     * ingredientes de OTRO negocio y ver sus movimientos de stock.
     */
    public List<MovimientoStockResponse> obtenerMovimientos(
            List<Long> ingredienteIds,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {
        return movimientoStockRepository
                .findByIngredientesAndFechaBetweenAndNegocioId(
                        ingredienteIds, fechaDesde, fechaHasta, SecurityUtils.getNegocioId())
                .stream()
                .map(MovimientoStockResponse::new)
                .collect(Collectors.toList());
    }
}
