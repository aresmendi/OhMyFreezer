package com.ares.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de elaboración de una receta.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElaborarRecetaRequest {

    /**
     * ID del usuario que elabora la receta.
     */
    private Long usuarioId;
}