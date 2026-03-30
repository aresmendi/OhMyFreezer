package com.ares.backend.dto;

import com.ares.backend.entity.MovimientoStock;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MovimientoStockResponse {
    private Long id;
    private Long ingredienteId;
    private String ingredienteNombre;
    private String ingredienteUnidad;
    private Double cantidadAnterior;
    private Double cantidadNueva;
    private Double cantidadCambio;
    private String tipo;
    private LocalDateTime fecha;
    private String motivo;

    public MovimientoStockResponse(MovimientoStock movimiento) {
        this.id = movimiento.getId();
        this.ingredienteId = movimiento.getIngrediente().getId();
        this.ingredienteNombre = movimiento.getIngrediente().getNombre();
        this.ingredienteUnidad = movimiento.getIngrediente().getUnidadMedida();
        this.cantidadAnterior = movimiento.getCantidadAnterior();
        this.cantidadNueva = movimiento.getCantidadNueva();
        this.cantidadCambio = movimiento.getCantidadCambio();
        this.tipo = movimiento.getTipo();
        this.fecha = movimiento.getFecha();
        this.motivo = movimiento.getMotivo();
    }
}
