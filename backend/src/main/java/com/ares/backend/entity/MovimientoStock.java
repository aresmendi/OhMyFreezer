package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece este movimiento. A diferencia de las
     * demás entidades tenant-owned, esta columna es propia (no se puede
     * derivar de usuarioId, que es un Long crudo sin FK), por lo que se
     * setea explícitamente al crear el movimiento.
     * NOTA: nullable de forma transitoria hasta el service retrofit;
     * la constraint NOT NULL real vive en la migración V2 (BD).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(nullable = false)
    private Double cantidadAnterior;

    @Column(nullable = false)
    private Double cantidadNueva;

    @Column(nullable = false)
    private Double cantidadCambio;

    @Column(nullable = false, length = 20)
    private String tipo; // "ENTRADA" o "SALIDA"

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String motivo;

    @Column(nullable = false)
    private Long usuarioId;

    public MovimientoStock(Ingrediente ingrediente, Double cantidadAnterior, Double cantidadNueva, String tipo, String motivo, Long usuarioId) {
        this.ingrediente = ingrediente;
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
        this.cantidadCambio = cantidadNueva - cantidadAnterior;
        this.tipo = tipo;
        this.fecha = LocalDateTime.now();
        this.motivo = motivo;
        this.usuarioId = usuarioId;
    }
}
