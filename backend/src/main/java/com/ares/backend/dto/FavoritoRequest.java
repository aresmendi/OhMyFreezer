package com.ares.backend.dto;

import lombok.*;

/**
 * DTO para peticiones relacionadas con favoritos.
 * Se usa tanto para marcar como para desmarcar favoritos.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritoRequest {

    /**
     * ID de la receta a marcar/desmarcar como favorita.
     */
    private Long recetaId;
}