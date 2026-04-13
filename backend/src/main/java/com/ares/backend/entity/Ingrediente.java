package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad que representa un ingrediente disponible en el congelador/despensa.
 * Controla el stock actual y el stock mínimo para generar alertas.
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
@Table(name = "ingredientes")
public class Ingrediente {

    /**
     * Identificador único del ingrediente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del ingrediente (ej: Tomate, Queso, Pasta).
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Cantidad actual disponible del ingrediente.
     */
    @Column(nullable = false)
    private Double cantidad;

    /**
     * Unidad de medida del ingrediente (gramos, litros, unidades, etc.).
     */
    @Column(nullable = false, length = 50)
    private String unidadMedida;

    /**
     * Stock mínimo requerido. Si la cantidad cae por debajo, se genera una alerta.
     */
    @Column(nullable = false)
    private Double stockMinimo;

    /**
     * Fecha y hora de la última actualización del ingrediente.
     */
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    /**
     * Constructor con parámetros para crear un ingrediente.
     *
     * @param nombre Nombre del ingrediente
     * @param cantidad Cantidad inicial disponible
     * @param unidadMedida Unidad de medida (gramos, litros, etc.)
     * @param stockMinimo Stock mínimo para alertas
     */
    public Ingrediente(String nombre, Double cantidad, String unidadMedida, Double stockMinimo) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.stockMinimo = stockMinimo;
        this.fechaActualizacion = LocalDateTime.now();
    }

    /**
     * Verifica si el ingrediente tiene stock bajo (cantidad menor al mínimo).
     *
     * @return true si el stock está por debajo del mínimo
     */
    public boolean tieneStockBajo() {
        return this.cantidad < this.stockMinimo;
    }

    /**
     * Elimina todos los registros relacionados con Ingrediente
     */
    @OneToMany(mappedBy = "ingrediente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimientoStock> movimientos;
}