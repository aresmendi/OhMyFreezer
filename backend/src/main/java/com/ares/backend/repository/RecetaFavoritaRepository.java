package com.ares.backend.repository;

import com.ares.backend.entity.RecetaFavorita;
import com.ares.backend.entity.Usuario;
import com.ares.backend.entity.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad RecetaFavorita.
 * Proporciona métodos para gestionar favoritos de usuarios.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface RecetaFavoritaRepository extends JpaRepository<RecetaFavorita, Long> {

    /**
     * Busca todas las recetas favoritas de un usuario.
     * Ordenadas por fecha de marcado (más recientes primero).
     *
     * @param usuario Usuario del que buscar favoritos
     * @return Lista de favoritos del usuario
     */
    List<RecetaFavorita> findByUsuarioOrderByFechaMarcadoDesc(Usuario usuario);

    /**
     * Busca un favorito específico de un usuario para una receta.
     *
     * @param usuario Usuario que marcó el favorito
     * @param receta Receta marcada como favorita
     * @return Optional con el favorito si existe
     */
    Optional<RecetaFavorita> findByUsuarioAndReceta(Usuario usuario, Receta receta);

    /**
     * Verifica si un usuario tiene una receta marcada como favorita.
     *
     * @param usuario Usuario a verificar
     * @param receta Receta a verificar
     * @return true si existe el favorito, false en caso contrario
     */
    boolean existsByUsuarioAndReceta(Usuario usuario, Receta receta);

    /**
     * Elimina un favorito de un usuario para una receta específica.
     *
     * @param usuario Usuario que tiene el favorito
     * @param receta Receta a desmarcar
     */
    void deleteByUsuarioAndReceta(Usuario usuario, Receta receta);

    /**
     * Cuenta cuántos favoritos tiene un usuario.
     *
     * @param usuario Usuario del que contar favoritos
     * @return Número de recetas favoritas del usuario
     */
    long countByUsuario(Usuario usuario);

    /**
     * Obtiene los IDs de las recetas favoritas de un usuario.
     * Útil para marcar recetas como favoritas en listados.
     *
     * @param usuarioId ID del usuario
     * @return Lista de IDs de recetas favoritas
     */
    @Query("SELECT rf.receta.id FROM RecetaFavorita rf WHERE rf.usuario.id = :usuarioId")
    List<Long> findRecetaIdsByUsuarioId(@Param("usuarioId") Long usuarioId);
}