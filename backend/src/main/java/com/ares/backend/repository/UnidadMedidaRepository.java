package com.ares.backend.repository;

import com.ares.backend.entity.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad UnidadMedida.
 * Catálogo global sin scoping por negocio: {@code findAll()} devuelve
 * siempre el mismo conjunto de filas para cualquier tenant.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {

    /**
     * Busca una unidad de medida por su código exacto (ej: "kg").
     * Usado para resolver el campo legacy {@code unidadMedida} de Ingrediente
     * durante la ventana de compatibilidad.
     *
     * @param codigo código de la unidad
     * @return Optional con la unidad si existe
     */
    Optional<UnidadMedida> findByCodigo(String codigo);
}
