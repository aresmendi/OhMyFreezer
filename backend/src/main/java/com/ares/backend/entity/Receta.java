package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una receta de cocina.
 * Contiene los pasos a seguir y los ingredientes necesarios.
 * Solo puede ser creada y modificada por jefes de cocina.
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
@Table(name = "recetas")
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class Receta {

    /**
     * Identificador único de la receta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece esta receta. Las tablas hijas
     * (pasos_receta, receta_ingredientes, recetas_favoritas) se scopean
     * transitivamente a través de esta receta, sin columna propia.
     * Endurecido a nullable=false en Fase 9/PR6, una vez verificado que
     * RecetaService setea siempre negocio al crear — consistente con la
     * constraint NOT NULL de la migración V2 (BD).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Nombre de la receta (ej: Pasta Carbonara, Ensalada César).
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Descripción general de la receta.
     */
    @Column(length = 500)
    private String descripcion;

    /**
     * Fecha y hora de creación de la receta.
     */
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Usuario (jefe de cocina) que creó la receta.
     */
    @ManyToOne
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    /**
     * Lista de pasos ordenados para elaborar la receta.
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasoReceta> pasos = new ArrayList<>();

    /**
     * Lista de ingredientes necesarios con sus cantidades.
     */
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecetaIngrediente> ingredientes = new ArrayList<>();

    /**
     * Constructor con parámetros para crear una receta.
     *
     * @param nombre Nombre de la receta
     * @param descripcion Descripción de la receta
     * @param creadaPor Usuario jefe de cocina que crea la receta
     */
    public Receta(String nombre, String descripcion, Usuario creadaPor) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadaPor = creadaPor;
        this.fechaCreacion = LocalDateTime.now();
    }
}