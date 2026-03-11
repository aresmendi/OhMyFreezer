package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad intermedia que relaciona una receta con sus ingredientes necesarios.
 * Especifica la cantidad exacta de cada ingrediente requerida.
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
@Table(name = "receta_ingredientes")
public class RecetaIngrediente {

    /**
     * Identificador único de la relación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Receta que requiere el ingrediente.
     */
    @ManyToOne
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    /**
     * Ingrediente necesario para la receta.
     */
    @ManyToOne
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    /**
     * Cantidad necesaria del ingrediente para elaborar la receta.
     * Usa la misma unidad de medida que el ingrediente.
     */
    @Column(nullable = false)
    private Double cantidadNecesaria;

    /**
     * Constructor con parámetros para crear una relación receta-ingrediente.
     *
     * @param receta Receta que requiere el ingrediente
     * @param ingrediente Ingrediente necesario
     * @param cantidadNecesaria Cantidad requerida
     */
    public RecetaIngrediente(Receta receta, Ingrediente ingrediente, Double cantidadNecesaria) {
        this.receta = receta;
        this.ingrediente = ingrediente;
        this.cantidadNecesaria = cantidadNecesaria;
    }
}