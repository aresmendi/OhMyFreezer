package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

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
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class RegistroUsoReceta {

    /**
     * Identificador único del registro.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece este registro de uso.
     * NOTA: nullable de forma transitoria hasta el service retrofit;
     * la constraint NOT NULL real vive en la migración V2 (BD).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id")
    private Negocio negocio;

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