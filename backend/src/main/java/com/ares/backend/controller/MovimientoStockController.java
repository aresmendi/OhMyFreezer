package com.ares.backend.controller;

import com.ares.backend.dto.MovimientoStockResponse;
import com.ares.backend.service.MovimientoStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockService movimientoStockService;

    @GetMapping
    public ResponseEntity<List<MovimientoStockResponse>> obtenerMovimientos(
            @RequestParam List<Long> ingredienteIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {
        return ResponseEntity.ok(movimientoStockService.obtenerMovimientos(ingredienteIds, fechaDesde, fechaHasta));
    }
}
