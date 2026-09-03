package com.ares.backend.repository;

import com.ares.backend.entity.TpvApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad TpvApiKey.
 *
 * @author Ares
 * @version 1.0
 */
@Repository
public interface TpvApiKeyRepository extends JpaRepository<TpvApiKey, Long> {

    /**
     * Resuelve la credencial ACTIVA que coincide con el prefijo indicado.
     * Es el lookup O(1) que usa {@code TpvApiKeyFilter}/
     * {@code TpvAutenticacionService} (D-D del diseño): un índice único
     * sobre el prefijo en claro, seguido de un único bcrypt.matches sobre
     * el secreto — nunca un escaneo de todas las credenciales activas.
     * Una credencial revocada nunca se devuelve, aunque el prefijo exista.
     *
     * @param prefijo Prefijo público de la credencial (parte de
     *                {@code omf_tpv_<prefijo>_<secreto>})
     * @return Optional con la credencial activa si existe
     */
    Optional<TpvApiKey> findByPrefijoAndActivaTrue(String prefijo);
}
