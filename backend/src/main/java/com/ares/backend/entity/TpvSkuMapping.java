package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * Mapeo entre un código de producto del TPV ({@link #skuTpv}) y una
 * {@link Receta} del negocio. Vive en una tabla propia, no en una columna
 * de {@code Receta} (D-B del diseño): un TPV real puede emitir varios SKUs
 * para el mismo plato (medias raciones, menús, variantes), y una fila con
 * {@link #receta} en null ES la fila "pendiente de mapear" que ve el jefe
 * de cocina en pantalla — no hace falta un estado ni una tabla aparte.
 * <p>
 * {@code sku_tpv} es único por negocio, nunca global: dos negocios
 * distintos pueden usar el mismo código de producto sin colisionar.
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
@Table(name = "tpv_sku_mapping")
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class TpvSkuMapping {

    /**
     * Identificador único del mapeo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece este mapeo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Código de producto tal como lo emite el TPV. Único por negocio
     * (constraint compuesta {@code UNIQUE(negocio_id, sku_tpv)}), nunca
     * global.
     */
    @Column(nullable = false, length = 64)
    private String skuTpv;

    /**
     * Etiqueta del producto tal como aparece en el TPV, informativa
     * (opcional): ayuda al jefe de cocina a identificar qué mapear.
     */
    @Column(length = 120)
    private String nombreTpv;

    /**
     * Receta a la que resuelve este SKU. NULLABLE a propósito: null es la
     * fila "pendiente de mapear" (D-B del diseño), no un estado separado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    /**
     * Fecha y hora de creación del mapeo (o de la fila pendiente, si se
     * creó automáticamente al ver un SKU desconocido por primera vez).
     */
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de la última actualización (asignar, editar o
     * desmapear la receta).
     */
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Constructor con parámetros para crear un mapeo (o una fila
     * pendiente, pasando {@code receta = null}).
     *
     * @param negocio   Negocio (tenant) al que pertenece
     * @param skuTpv    Código de producto del TPV
     * @param nombreTpv Etiqueta informativa del producto (opcional)
     * @param receta    Receta a la que resuelve, o null si está pendiente
     */
    public TpvSkuMapping(Negocio negocio, String skuTpv, String nombreTpv, Receta receta) {
        this.negocio = negocio;
        this.skuTpv = skuTpv;
        this.nombreTpv = nombreTpv;
        this.receta = receta;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}
