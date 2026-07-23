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
     * Busca un usuario por su email, identificador GLOBAL de login (único en
     * todo el sistema desde la migración V3). Reemplaza a la búsqueda por
     * username junto con la desambiguación por contraseña que usaba
     * {@code login()} antes de esta migración: al ser el email único
     * globalmente, no hace falta desambiguar nada, cerrando de raíz la fuga
     * cross-tenant donde dos negocios con el mismo username Y la misma
     * contraseña podían autenticar al usuario equivocado.
     *
     * @param email Correo electrónico a buscar
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario con ese email en CUALQUIER negocio
     * del sistema (el email es único globalmente, no por negocio). Se usa en
     * el alta (tanto {@code registrar()} como {@code crearEmpleado()}) para
     * rechazar duplicados con un error de validación claro, en vez de dejar
     * que la constraint UNIQUE de base de datos lo haga como un 500.
     *
     * @param email Correo electrónico a verificar
     * @return true si ya existe un usuario con ese email
     */
    boolean existsByEmail(String email);

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

    /**
     * Verifica si existe un usuario con ese username dentro de un negocio
     * (tenant) concreto. Reemplaza a {@link #existsByUsername(String)} en el
     * registro/alta: la unicidad de username ya no es global, es por negocio
     * (constraint compuesta UNIQUE(negocio_id, username)).
     *
     * @param username  Nombre de usuario a verificar
     * @param negocioId ID del negocio (tenant) contra el que verificar
     * @return true si ya existe ese username en ese negocio
     */
    boolean existsByUsernameAndNegocioId(String username, Long negocioId);

    /**
     * Busca un usuario por su id, scoped al negocio (tenant) indicado.
     * Reemplaza al {@code findById} plano en {@code UsuarioService.eliminar()}:
     * sin este scoping, un jefe de un negocio podía eliminar empleados de
     * OTRO negocio pasando su id (fuga cross-tenant descubierta en la Fase 8
     * de tests de aislamiento).
     *
     * @param id        id del usuario a buscar
     * @param negocioId id del negocio (tenant) del caller autenticado
     * @return Optional con el usuario si existe y pertenece a ese negocio
     */
    Optional<Usuario> findByIdAndNegocioId(Long id, Long negocioId);
}