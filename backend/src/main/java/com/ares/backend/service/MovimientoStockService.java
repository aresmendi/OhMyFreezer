package com.ares.backend.service;

import com.ares.backend.dto.MovimientoStockResponse;
import com.ares.backend.entity.MovimientoStock;
import com.ares.backend.repository.MovimientoStockRepository;
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

    @Transactional
    public void registrarMovimiento(
            com.ares.backend.entity.Ingrediente ingrediente,
            Double cantidadAnterior,
            Double cantidadNueva,
            String tipo,
            String motivo,
            Long usuarioId) {
        MovimientoStock movimiento = new MovimientoStock(
                ingrediente, cantidadAnterior, cantidadNueva, tipo, motivo, usuarioId);
        movimientoStockRepository.save(movimiento);
    }

    public List<MovimientoStockResponse> obtenerMovimientos(
            List<Long> ingredienteIds,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta) {
        return movimientoStockRepository
                .findByIngredientesAndFechaBetween(ingredienteIds, fechaDesde, fechaHasta)
                .stream()
                .map(MovimientoStockResponse::new)
                .collect(Collectors.toList());
    }
}
