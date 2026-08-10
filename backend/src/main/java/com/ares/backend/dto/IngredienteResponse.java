package com.ares.backend.dto;

import com.ares.backend.entity.Ingrediente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con información de un ingrediente.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteResponse {

    /**
     * Identificador único del ingrediente.
     */
    private Long id;

    /**
     * Nombre del ingrediente.
     */
    private String nombre;

    /**
     * Cantidad disponible.
     */
    private Double cantidad;

    /**
     * Unidad de medida, como código legado de texto (ej. "kg"). Se mantiene
     * como proyección de lectura de compatibilidad: el servicio la
     * sincroniza con el código de {@link #unidad} en cada escritura, así que
     * los clientes que solo leen este campo (pre-migración) no se ven
     * afectados.
     */
    private String unidadMedida;

    /**
     * Identificador de la unidad de medida en el catálogo global
     * {@code unidades_medida}. {@code null} si el ingrediente todavía no
     * tiene unidad tipada asignada.
     */
    private Long unidadBaseId;

    /**
     * Unidad de medida tipada del catálogo global (código, tipo, factor de
     * conversión). {@code null} si el ingrediente todavía no tiene unidad
     * tipada asignada.
     */
    private UnidadMedidaResponse unidad;

    /**
     * Stock mínimo.
     */
    private Double stockMinimo;

    /**
     * Indica si el ingrediente tiene stock bajo.
     */
    private Boolean alertaBajo;

    /**
     * Fecha de última actualización.
     */
    private LocalDateTime fechaActualizacion;

    /**
     * Constructor que convierte una entidad Ingrediente a IngredienteResponse.
     *
     * @param ingrediente Entidad Ingrediente a convertir
     */
    public IngredienteResponse(Ingrediente ingrediente) {
        this.id = ingrediente.getId();
        this.nombre = ingrediente.getNombre();
        this.cantidad = ingrediente.getCantidad();
        this.unidadMedida = ingrediente.getUnidadMedida();
        this.stockMinimo = ingrediente.getStockMinimo();
        this.alertaBajo = ingrediente.tieneStockBajo();
        this.fechaActualizacion = ingrediente.getFechaActualizacion();
        if (ingrediente.getUnidadBase() != null) {
            this.unidadBaseId = ingrediente.getUnidadBase().getId();
            this.unidad = new UnidadMedidaResponse(ingrediente.getUnidadBase());
        }
    }
}