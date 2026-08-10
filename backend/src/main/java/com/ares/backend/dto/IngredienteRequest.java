package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de creación o actualización de un ingrediente.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredienteRequest {

    /**
     * Nombre del ingrediente.
     */
    private String nombre;

    /**
     * Cantidad disponible del ingrediente.
     */
    private Double cantidad;

    /**
     * Unidad de medida (gramos, litros, unidades, etc.), como código legado
     * de texto libre (ej. "kg"). Campo de compatibilidad: se tolera durante
     * la ventana de deprecación, pero {@link #unidadBaseId} tiene prioridad
     * cuando ambos vienen informados (ver Fase 2 "unidades-medida").
     */
    private String unidadMedida;

    /**
     * Identificador de la unidad de medida en el catálogo global
     * {@code unidades_medida}. Preferido sobre {@link #unidadMedida}: si
     * viene informado, resuelve la unidad directamente por id.
     */
    private Long unidadBaseId;

    /**
     * Stock mínimo para generar alertas.
     */
    private Double stockMinimo;
}