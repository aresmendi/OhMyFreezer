package com.ares.backend.dto;

import com.ares.backend.entity.RecetaFavorita;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para favoritos.
 * Incluye información completa de la receta marcada como favorita.
 *
 * @author Ares
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecetaFavoritaResponse {

    /**
     * ID del favorito.
     */
    private Long id;

    /**
     * Datos completos de la receta favorita.
     */
    private RecetaDetailResponse receta;

    /**
     * Fecha en que se marcó como favorita.
     */
    private LocalDateTime fechaMarcado;

    /**
     * Constructor desde entidad RecetaFavorita.
     *
     * @param favorito Entidad RecetaFavorita
     * @param recetaDetail Detalles de la receta
     */
    public RecetaFavoritaResponse(RecetaFavorita favorito, RecetaDetailResponse recetaDetail) {
        this.id = favorito.getId();
        this.receta = recetaDetail;
        this.fechaMarcado = favorito.getFechaMarcado();
    }
}