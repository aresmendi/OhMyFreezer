package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa un Negocio (tenant) dentro del sistema OhMyFreezer.
 * Todas las demás entidades tenant-owned (usuarios, ingredientes, recetas,
 * alertas, movimientos de stock y registros de uso) pertenecen exactamente
 * a un Negocio a través de su columna negocio_id.
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
@Table(name = "negocios")
public class Negocio {

    /**
     * Identificador único del negocio.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre comercial del negocio.
     */
    @Column(nullable = false, length = 100)
    private String nombre;

    /**
     * Plan de suscripción del negocio (por ahora siempre 'FREE').
     */
    @Column(nullable = false, length = 30)
    private String plan = "FREE";

    /**
     * Fecha y hora de alta del negocio en el sistema.
     */
    @Column(nullable = false)
    private LocalDateTime fechaAlta;

    /**
     * Email de contacto del negocio (opcional).
     */
    @Column(length = 100)
    private String emailContacto;

    /**
     * Constructor con parámetros para dar de alta un negocio.
     *
     * @param nombre        Nombre comercial del negocio
     * @param emailContacto Email de contacto (opcional)
     */
    public Negocio(String nombre, String emailContacto) {
        this.nombre = nombre;
        this.emailContacto = emailContacto;
        this.plan = "FREE";
        this.fechaAlta = LocalDateTime.now();
    }
}
