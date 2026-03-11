package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que representa un paso individual de una receta.
 * Los pasos están ordenados secuencialmente para guiar la elaboración.
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
@Table(name = "pasos_receta")
public class PasoReceta {

    /**
     * Identificador único del paso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Orden del paso en la secuencia de la receta (1, 2, 3...).
     */
    @Column(nullable = false)
    private Integer orden;

    /**
     * Descripción detallada del paso a realizar.
     */
    @Column(nullable = false, length = 500)
    private String descripcion;

    /**
     * Receta a la que pertenece este paso.
     */
    @ManyToOne
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    /**
     * Constructor con parámetros para crear un paso de receta.
     *
     * @param orden Orden del paso en la secuencia
     * @param descripcion Descripción del paso
     * @param receta Receta a la que pertenece
     */
    public PasoReceta(Integer orden, String descripcion, Receta receta) {
        this.orden = orden;
        this.descripcion = descripcion;
        this.receta = receta;
    }
}