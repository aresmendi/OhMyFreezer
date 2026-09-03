package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de un evento (venta o anulación) ingerido desde un TPV. Es la
 * fila de idempotencia de la ingesta (D-A del diseño):
 * {@code reservarIdempotencia()} la inserta con estado {@code RECIBIDA}
 * ANTES de tocar stock, así que un crash a mitad de flujo deja una fila
 * visible "en duda" en vez de perderse silenciosamente.
 * <p>
 * {@link #fechaTpv} es meramente informativa (viene del payload del
 * proveedor); {@link #fechaRecepcion} es la autoritativa — es la que se
 * usa para datar la reversión de stock de una anulación, nunca la fecha
 * de la venta original (así una corrección de hace semanas nunca toca un
 * periodo de reporte ya cerrado).
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
@Table(name = "ventas_tpv")
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class VentaTpv {

    /**
     * Identificador único del evento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece este evento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Tipo de evento: VENTA o ANULACION.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoEventoTpv tipo;

    /**
     * Identidad de idempotencia propia de ESTE evento. Único por negocio
     * ({@code uk_ventas_tpv_external}): replays con el mismo valor
     * violan la constraint y responden {@code DUPLICADA}.
     */
    @Column(nullable = false, length = 128)
    private String externalId;

    /**
     * Solo presente en eventos ANULACION: el {@link #externalId} de la
     * venta que se está anulando. Único por negocio
     * ({@code uk_ventas_tpv_original}): una venta no puede anularse dos
     * veces. NULL en eventos VENTA — MySQL/H2 tratan NULL como distinto en
     * un índice único, así que las ventas normales nunca colisionan por
     * esta columna.
     */
    private String externalIdOriginal;

    /**
     * Código de producto del TPV tal como llegó en el payload.
     */
    @Column(nullable = false, length = 64)
    private String skuTpv;

    /**
     * Cantidad vendida (o anulada) de ese SKU en este evento.
     */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Receta resuelta para {@link #skuTpv} en el momento de procesar este
     * evento, si el SKU estaba mapeado. NULL si el evento quedó en
     * {@code SIN_MAPEO}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_id")
    private Receta receta;

    /**
     * Resultado del procesamiento de este evento (ver {@link EstadoVentaTpv}).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoVentaTpv estado;

    /**
     * Fecha/hora que reporta el propio TPV para este evento. Meramente
     * informativa; nunca se usa para datar movimientos de stock.
     */
    private LocalDateTime fechaTpv;

    /**
     * Fecha/hora de recepción en el servidor. Autoritativa: es la que
     * data la reversión de stock de una anulación.
     */
    @Column(nullable = false)
    private LocalDateTime fechaRecepcion;

    /**
     * Detalle libre del resultado (por ejemplo, el motivo de un
     * SIN_STOCK/SIN_MAPEO), opcional.
     */
    @Column(length = 500)
    private String detalle;

    /**
     * Snapshot medido del stock realmente descontado por esta venta, una
     * línea por ingrediente (D-C del diseño). Vacío en eventos ANULACION
     * y en ventas que no llegaron a descontar stock (SIN_STOCK, SIN_MAPEO).
     */
    @OneToMany(mappedBy = "ventaTpv", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaTpvLinea> lineas = new ArrayList<>();

    /**
     * Constructor con parámetros para registrar un evento TPV.
     *
     * @param negocio            Negocio (tenant) al que pertenece
     * @param tipo               VENTA o ANULACION
     * @param externalId         Identidad de idempotencia de este evento
     * @param externalIdOriginal externalId de la venta original (solo ANULACION), o null
     * @param skuTpv             Código de producto del TPV
     * @param cantidad           Cantidad del evento
     * @param estado             Resultado del procesamiento
     */
    public VentaTpv(Negocio negocio, TipoEventoTpv tipo, String externalId, String externalIdOriginal,
                     String skuTpv, Integer cantidad, EstadoVentaTpv estado) {
        this.negocio = negocio;
        this.tipo = tipo;
        this.externalId = externalId;
        this.externalIdOriginal = externalIdOriginal;
        this.skuTpv = skuTpv;
        this.cantidad = cantidad;
        this.estado = estado;
        this.fechaRecepcion = LocalDateTime.now();
    }
}
