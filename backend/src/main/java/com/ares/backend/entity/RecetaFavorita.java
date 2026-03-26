package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una receta marcada como favorita por un usuario.
 *
 * @author Ares
 * @version 1.0
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name="recetas_favoritas")
public class RecetaFavorita {

    /**
     * Identificador único del favorito.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que marcó la receta como favorita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Receta marcada como favorita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    /**
     * Fecha y hora en que se marcó como favorita.
     */
    @Column(nullable = false)
    private LocalDateTime fechaMarcado;

    /**
     * Constructor con parámetros para crear un favorito.
     *
     * @param usuario Usuario que marca como favorita
     * @param receta Receta marcada como favorita
     */
    public RecetaFavorita(Usuario usuario, Receta receta) {
        this.usuario = usuario;
        this.receta = receta;
        this.fechaMarcado = LocalDateTime.now();
    }
}