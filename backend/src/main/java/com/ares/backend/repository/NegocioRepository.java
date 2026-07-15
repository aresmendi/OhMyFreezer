package com.ares.backend.repository;

import com.ares.backend.entity.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Negocio.
 * Proporciona operaciones CRUD básicas sobre los negocios (tenants).
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Long> {
}
