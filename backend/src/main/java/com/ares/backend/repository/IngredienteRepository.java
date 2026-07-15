package com.ares.backend.repository;

import com.ares.backend.entity.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Ingrediente.
 * Proporciona métodos para acceder y gestionar ingredientes en la base de datos.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {

    /**
     * Busca un ingrediente por su nombre.
     *
     * @param nombre Nombre del ingrediente a buscar
     * @return Optional con el ingrediente si existe
     */
    Optional<Ingrediente> findByNombre(String nombre);

    /**
     * Busca todos los ingredientes cuya cantidad está por debajo del stock mínimo.
     * Útil para generar alertas de stock bajo.
     *
     * @return Lista de ingredientes con stock bajo
     */
    @Query("SELECT i FROM Ingrediente i WHERE i.cantidad < i.stockMinimo")
    List<Ingrediente> findIngredientesConStockBajo();

    /**
     * Busca ingredientes por unidad de medida.
     *
     * @param unidadMedida Unidad de medida a buscar (gramos, litros, etc.)
     * @return Lista de ingredientes con esa unidad de medida
     */
    List<Ingrediente> findByUnidadMedida(String unidadMedida);

    /**
     * Verifica si existe un ingrediente con el nombre dado (ignorando mayúsculas/minúsculas).
     *
     * @param nombre Nombre del ingrediente a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Busca un ingrediente por su ID, scoped al negocio (tenant) del caller.
     * Un id perteneciente a otro negocio no puede cargarse por esta vía:
     * el Optional viene vacío exactamente igual que si el id no existiera.
     *
     * @param id ID del ingrediente
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return Optional con el ingrediente si existe y pertenece a ese negocio
     */
    Optional<Ingrediente> findByIdAndNegocioId(Long id, Long negocioId);

    /**
     * Verifica si existe un ingrediente con el nombre dado dentro de un negocio
     * específico (ignorando mayúsculas/minúsculas). Reemplaza a
     * {@link #existsByNombreIgnoreCase(String)} en los flujos de escritura:
     * la unicidad de nombre es per-negocio, no global.
     *
     * @param nombre Nombre del ingrediente a verificar
     * @param negocioId ID del negocio (tenant) del caller autenticado
     * @return true si existe dentro de ese negocio, false en caso contrario
     */
    boolean existsByNombreIgnoreCaseAndNegocioId(String nombre, Long negocioId);
}