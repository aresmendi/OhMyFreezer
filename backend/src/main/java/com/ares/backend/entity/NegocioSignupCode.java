package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Código de alta (signup code) de un Negocio. Se provisiona manualmente,
 * es de un solo uso y resuelve a exactamente un Negocio. El primer jefe de
 * cocina lo consume al registrarse; a partir de ahí queda marcado como
 * usado. Un administrador puede revocarlo antes de tiempo poniendo
 * activo=false.
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
@Table(name = "negocio_signup_codes")
public class NegocioSignupCode {

    /**
     * Identificador único del código de alta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio al que resuelve este código de alta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Código de alta, único en todo el sistema.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String codigo;

    /**
     * Indica si el código ya fue consumido (single-use).
     */
    @Column(nullable = false)
    private Boolean usado = false;

    /**
     * Indica si el código sigue activo. Un admin puede ponerlo en false
     * para revocarlo antes de que se use.
     */
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Id del usuario (jefe) que consumió el código, si ya se usó.
     */
    @Column(name = "usado_por_usuario_id")
    private Long usadoPorUsuarioId;

    /**
     * Fecha y hora en que se creó (provisionó) el código.
     */
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora en que se consumió el código, si ya se usó.
     */
    private LocalDateTime fechaUso;

    /**
     * Constructor con parámetros para provisionar un código de alta.
     *
     * @param negocio Negocio al que resuelve el código
     * @param codigo  Código de alta a asignar
     */
    public NegocioSignupCode(Negocio negocio, String codigo) {
        this.negocio = negocio;
        this.codigo = codigo;
        this.usado = false;
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Un código es válido para consumirse sólo si sigue activo (no
     * revocado por un admin) y todavía no fue usado (single-use).
     *
     * @return true si el código puede consumirse en un registro
     */
    public boolean esValido() {
        return Boolean.TRUE.equals(this.activo) && Boolean.FALSE.equals(this.usado);
    }

    /**
     * Marca el código como consumido por el usuario indicado.
     *
     * @param usuarioId id del usuario (jefe) que consumió el código
     */
    public void marcarUsado(Long usuarioId) {
        this.usado = true;
        this.usadoPorUsuarioId = usuarioId;
        this.fechaUso = LocalDateTime.now();
    }
}
