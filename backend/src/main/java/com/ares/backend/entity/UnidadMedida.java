package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad de catálogo global de unidades de medida.
 * Deliberadamente NO lleva asociación a {@link Negocio} ni la anotación
 * {@code @Filter}: es una tabla de referencia (constantes físicas), idéntica
 * y compartida por todos los negocios (tenants). Ver decisión D1 del diseño
 * de la fase "unidades-medida".
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
@Table(name = "unidades_medida")
public class UnidadMedida {

    /**
     * Identificador único de la unidad de medida.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código corto de la unidad (ej: "kg", "g", "ml", "L", "ud").
     */
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    /**
     * Nombre descriptivo de la unidad (ej: "Kilogramo").
     */
    @Column(nullable = false, length = 50)
    private String nombre;

    /**
     * Dimensión física de la unidad (MASA, VOLUMEN o UNIDAD).
     * Dos unidades solo se pueden convertir entre sí si comparten tipo.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUnidad tipo;

    /**
     * Factor de conversión respecto a la unidad base de su tipo
     * (gramo para MASA, mililitro para VOLUMEN, unidad para UNIDAD).
     */
    @Column(name = "factor_a_base", nullable = false)
    private Double factorABase;
}
