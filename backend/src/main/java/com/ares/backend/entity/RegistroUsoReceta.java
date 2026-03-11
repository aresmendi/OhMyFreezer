package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que registra cada vez que se elabora una receta.
 * Permite llevar estadísticas de uso y consumo de ingredientes.
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
@Table(name = "registro_uso_recetas")
public class RegistroUsoReceta {

    /**
     * Identificador único del registro.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Receta que fue elaborada.
     */
    @ManyToOne
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    /**
     * Usuario que elaboró la receta.
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Fecha y hora en que se elaboró la receta.
     */
    @Column(nullable = false)
    private LocalDateTime fechaElaboracion;

    /**
     * Indica si la elaboración se completó exitosamente.
     */
    @Column(nullable = false)
    private Boolean completada = true;

    /**
     * Constructor con parámetros para crear un registro de uso.
     *
     * @param receta Receta elaborada
     * @param usuario Usuario que elaboró la receta
     * @param completada Sí se completó exitosamente
     */
    public RegistroUsoReceta(Receta receta, Usuario usuario, Boolean completada) {
        this.receta = receta;
        this.usuario = usuario;
        this.completada = completada;
        this.fechaElaboracion = LocalDateTime.now();
    }
}