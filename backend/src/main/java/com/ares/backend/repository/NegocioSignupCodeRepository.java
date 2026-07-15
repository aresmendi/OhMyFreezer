package com.ares.backend.repository;

import com.ares.backend.entity.NegocioSignupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad NegocioSignupCode.
 * Proporciona operaciones CRUD básicas sobre los códigos de alta de negocio.
 * Los finders de negocio-onboarding (búsqueda por código) se añaden en la
 * fase de onboarding, junto con UsuarioService.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface NegocioSignupCodeRepository extends JpaRepository<NegocioSignupCode, Long> {
}
