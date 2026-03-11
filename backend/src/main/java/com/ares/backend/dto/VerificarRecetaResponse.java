package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de verificación de disponibilidad de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificarRecetaResponse {

    /**
     * ID de la receta verificada.
     */
    private Long recetaId;

    /**
     * Indica si la receta está disponible (hay stock suficiente).
     */
    private Boolean disponible;

    /**
     * Lista de ingredientes faltantes (vacía si disponible es true).
     */
    private List<IngredienteFaltanteDTO> ingredientesFaltantes;
}