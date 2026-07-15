package com.ares.backend.repository;

import com.ares.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 * Proporciona métodos para acceder y gestionar usuarios en la base de datos.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username Nombre de usuario a buscar
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica si existe un usuario con el nombre de usuario dado.
     *
     * @param username Nombre de usuario a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByUsername(String username);

    /**
     * Busca todos los usuarios que son jefes de cocina.
     *
     * @return Lista de jefes de cocina
     */
    List<Usuario> findByEsJefeCocinaTrue();

    /**
     * Busca todos los usuarios que son jefes de cocina de un negocio (tenant)
     * específico. Reemplaza a {@link #findByEsJefeCocinaTrue()} en flujos que
     * notifican/distribuyen alertas: sin este scoping, un jefe de un negocio
     * recibiría (y sería notificado por email de) alertas de TODOS los
     * negocios del sistema.
     *
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Lista de jefes de cocina de ese negocio
     */
    List<Usuario> findByEsJefeCocinaTrueAndNegocioId(Long negocioId);
}