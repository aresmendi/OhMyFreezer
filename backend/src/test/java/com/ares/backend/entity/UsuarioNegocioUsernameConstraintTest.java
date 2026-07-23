package com.ares.backend.entity;

import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que la unicidad de username pasó de ser global a ser compuesta
 * por negocio (UNIQUE(negocio_id, username)), tanto en el esquema generado
 * por Hibernate (tests) como en la migración V2 (prod/TiDB, cubierto en
 * FlywayMigrationTest).
 */
@DataJpaTest
@Transactional
class UsuarioNegocioUsernameConstraintTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    void permiteElMismoUsernameEnDosNegociosDistintos() {
        Negocio negocioA = negocioRepository.save(new Negocio("Negocio A", null));
        Negocio negocioB = negocioRepository.save(new Negocio("Negocio B", null));

        Usuario admin1 = new Usuario("admin", "hash", false);
        admin1.setEmail("admin1@negocioA.test");
        admin1.setNegocio(negocioA);
        usuarioRepository.saveAndFlush(admin1);

        Usuario admin2 = new Usuario("admin", "hash", false);
        admin2.setEmail("admin2@negocioB.test");
        admin2.setNegocio(negocioB);
        usuarioRepository.saveAndFlush(admin2);

        assertThat(usuarioRepository.findAll()).hasSize(2);
    }

    @Test
    void rechazaUsernameDuplicadoDentroDelMismoNegocio() {
        Negocio negocioA = negocioRepository.save(new Negocio("Negocio A", null));

        Usuario admin1 = new Usuario("admin", "hash", false);
        admin1.setEmail("admin1@negocioA.test");
        admin1.setNegocio(negocioA);
        usuarioRepository.saveAndFlush(admin1);

        Usuario admin2 = new Usuario("admin", "hash", false);
        admin2.setEmail("admin2@negocioA.test");
        admin2.setNegocio(negocioA);

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(admin2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
