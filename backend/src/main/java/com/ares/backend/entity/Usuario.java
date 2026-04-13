package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa un usuario del sistema OhMyFreezer.
 * Puede ser un cocinero normal o un jefe de cocina con permisos especiales.
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
@Table(name = "usuarios")
public class Usuario {

    /**
     * Identificador único del usuario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de usuario único para el login.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Contraseña del usuario (almacenada en hash).
     */
    @Column(nullable = false)
    private String password;

    /**
     * Indica si el usuario tiene permisos de jefe de cocina.
     * Los jefes de cocina pueden crear, editar y eliminar recetas.
     */
    @Column(nullable = false)
    private Boolean esJefeCocina = false;

    /**
     * Fecha y hora de registro del usuario en el sistema.
     */
    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    /**
     * Correo electrónico del usuario para notificaciones.
     */
    @Column(length = 100)
    private String email;


    /**
     * Constructor con parámetros para crear un usuario.
     *
     * @param username Nombre de usuario único
     * @param password Contraseña (debe ser hasheada antes de pasar)
     * @param esJefeCocina Indica si es jefe de cocina
     */
    public Usuario(String username, String password, Boolean esJefeCocina) {
        this.username = username;
        this.password = password;
        this.esJefeCocina = esJefeCocina;
        this.fechaRegistro = LocalDateTime.now();
    }
}