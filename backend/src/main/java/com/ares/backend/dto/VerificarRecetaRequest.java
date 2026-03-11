package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de verificación de disponibilidad de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificarRecetaRequest {

    /**
     * ID del usuario que verifica la receta.
     */
    private Long usuarioId;
}