package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

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
@ToString(exclude = "password")
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(
        name = "uk_usuarios_negocio_username", columnNames = {"negocio_id", "username"}))
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class Usuario {

    /**
     * Identificador único del usuario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece este usuario. La unicidad del
     * username ya no es global: se recompone como UNIQUE(negocio_id, username).
     * Tanto {@code UsuarioService.registrar()} como
     * {@code UsuarioService.crearEmpleado()} setean siempre este campo antes
     * de persistir, por lo que el mapeo JPA lo refleja como obligatorio
     * (consistente con la constraint NOT NULL de la migración V2 en BD).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Nombre de usuario para el login. Único por negocio, no globalmente
     * (constraint compuesta UNIQUE(negocio_id, username) a nivel de BD).
     */
    @Column(nullable = false, length = 50)
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