package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
