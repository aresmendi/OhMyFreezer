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
     * Cantidad necesaria del ingrediente para elaborar la receta, expresada
     * en {@link #unidad} (no necesariamente la unidad del ingrediente — ver
     * {@link com.ares.backend.service.RecetaService} para la conversión).
     */
    @Column(nullable = false)
    private Double cantidadNecesaria;

    /**
     * Unidad de medida en la que se expresa {@link #cantidadNecesaria}
     * (catálogo global {@link UnidadMedida}). Por defecto es la propia
     * unidad base del ingrediente, pero puede diferir siempre que comparta
     * {@code tipo} — {@code RecetaService} valida esa compatibilidad al
     * crear/actualizar la receta (Fase 2 "unidades-medida", PR3, decisión
     * D4). No se marca {@code nullable = false} a nivel de anotación JPA por
     * el mismo motivo que {@link Ingrediente#getUnidadBase()}: el esquema de
     * test (H2, {@code ddl-auto=create-drop}) no ejecuta la migración V5,
     * que sí impone NOT NULL en BD real.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_id")
    private UnidadMedida unidad;

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