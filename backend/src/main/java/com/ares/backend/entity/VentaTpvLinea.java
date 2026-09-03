package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Línea de snapshot: cantidad de un {@link Ingrediente} realmente
 * descontada por una {@link VentaTpv}, en la unidad base del ingrediente
 * (D-C del diseño). Es lo único que una anulación restaura — nunca una
 * recomputación desde la receta en el momento de anular, que pudo haber
 * sido editada entre la venta y su anulación.
 * <p>
 * Sin {@code negocio_id} propio ni {@code @Filter}: se scopea
 * transitivamente a través de {@link #ventaTpv} (igual que
 * {@link RecetaIngrediente} se scopea a través de {@link Receta}).
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
@Table(name = "ventas_tpv_lineas")
public class VentaTpvLinea {

    /**
     * Identificador único de la línea.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Venta TPV a la que pertenece esta línea de snapshot.
     */
    @ManyToOne
    @JoinColumn(name = "venta_tpv_id", nullable = false)
    private VentaTpv ventaTpv;

    /**
     * Ingrediente descontado.
     */
    @ManyToOne
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    /**
     * Cantidad realmente descontada de este ingrediente, en su unidad
     * base, medida como delta antes/después de {@code elaborar()} (nunca
     * recalculada desde la receta).
     */
    @Column(nullable = false)
    private Double cantidadDescontada;

    /**
     * Constructor con parámetros para registrar una línea de snapshot.
     *
     * @param ventaTpv           Venta TPV a la que pertenece
     * @param ingrediente        Ingrediente descontado
     * @param cantidadDescontada Cantidad realmente descontada (unidad base)
     */
    public VentaTpvLinea(VentaTpv ventaTpv, Ingrediente ingrediente, Double cantidadDescontada) {
        this.ventaTpv = ventaTpv;
        this.ingrediente = ingrediente;
        this.cantidadDescontada = cantidadDescontada;
    }
}
