package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una alerta del sistema.
 * Se generan alertas por stock bajo o recetas no disponibles.
 * Solo los jefes de cocina reciben estas alertas.
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
@Table(name = "alertas")
public class Alerta {

    /**
     * Identificador único de la alerta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo de alerta: STOCK_BAJO o RECETA_NO_DISPONIBLE.
     */
    @Column(nullable = false, length = 50)
    private String tipo;

    /**
     * Mensaje descriptivo de la alerta.
     */
    @Column(nullable = false, length = 500)
    private String mensaje;

    /**
     * Receta relacionada con la alerta (opcional).
     */
    @ManyToOne
    @JoinColumn(name = "receta_id")
    private Receta receta;

    /**
     * Ingrediente relacionado con la alerta (opcional).
     */
    @ManyToOne
    @JoinColumn(name = "ingrediente_id")
    private Ingrediente ingrediente;

    /**
     * Usuario jefe de cocina destinatario de la alerta.
     */
    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    /**
     * Fecha y hora de creación de la alerta.
     */
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Indica si la alerta ha sido leída por el jefe de cocina.
     */
    @Column(nullable = false)
    private Boolean leida = false;

    /**
     * Constructor con parámetros para crear una alerta.
     *
     * @param tipo Tipo de alerta (STOCK_BAJO, RECETA_NO_DISPONIBLE)
     * @param mensaje Mensaje descriptivo
     * @param destinatario Usuario jefe de cocina que recibirá la alerta
     */
    public Alerta(String tipo, String mensaje, Usuario destinatario) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.destinatario = destinatario;
        this.fechaCreacion = LocalDateTime.now();
        this.leida = false;
    }
}