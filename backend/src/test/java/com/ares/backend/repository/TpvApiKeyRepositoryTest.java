package com.ares.backend.repository;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, contra una BD real (H2, {@code ddl-auto=create-drop}), el mapeo
 * de la entidad {@link TpvApiKey} y el finder
 * {@code findByPrefijoAndActivaTrue} usado por el filtro de autenticación
 * TPV (D-D del diseño): resolución O(1) por prefijo indexado, ignorando
 * cualquier credencial revocada.
 */
@DataJpaTest
@Transactional
class TpvApiKeyRepositoryTest {

    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TpvApiKeyRepository tpvApiKeyRepository;

    private Negocio negocio;
    private Usuario usuarioSistema;

    @BeforeEach
    void crearFixtures() {
        negocio = negocioRepository.save(new Negocio("Negocio TPV", "tpv@ares.dev"));
        Usuario u = new Usuario("tpv-system", "sentinel", false);
        u.setEmail("tpv+negocio-1@tpv.ohmyfreezer.invalid");
        u.setNegocio(negocio);
        usuarioSistema = usuarioRepository.save(u);
    }

    @Test
    @DisplayName("findByPrefijoAndActivaTrue devuelve la credencial activa que coincide con el prefijo")
    void findByPrefijoAndActivaTrueDevuelveCredencialActiva() {
        TpvApiKey clave = new TpvApiKey(negocio, "pfx-abc123", "hash-bcrypt", usuarioSistema);
        tpvApiKeyRepository.save(clave);

        Optional<TpvApiKey> encontrada = tpvApiKeyRepository.findByPrefijoAndActivaTrue("pfx-abc123");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getSecretoHash()).isEqualTo("hash-bcrypt");
        assertThat(encontrada.get().getUsuarioSistema().getId()).isEqualTo(usuarioSistema.getId());
    }

    @Test
    @DisplayName("findByPrefijoAndActivaTrue no devuelve una credencial revocada, aunque el prefijo coincida")
    void findByPrefijoAndActivaTrueIgnoraCredencialRevocada() {
        TpvApiKey clave = new TpvApiKey(negocio, "pfx-revocada", "hash-bcrypt", usuarioSistema);
        clave.revocar();
        tpvApiKeyRepository.save(clave);

        Optional<TpvApiKey> encontrada = tpvApiKeyRepository.findByPrefijoAndActivaTrue("pfx-revocada");

        assertThat(encontrada).isEmpty();
    }

    @Test
    @DisplayName("findByPrefijoAndActivaTrue devuelve vacío para un prefijo inexistente")
    void findByPrefijoAndActivaTrueDevuelveVacioParaPrefijoInexistente() {
        Optional<TpvApiKey> encontrada = tpvApiKeyRepository.findByPrefijoAndActivaTrue("no-existe");

        assertThat(encontrada).isEmpty();
    }

    @Test
    @DisplayName("revocar() apaga activa y sella fechaRevocacion")
    void revocarApagaActivaYSellaFecha() {
        TpvApiKey clave = new TpvApiKey(negocio, "pfx-a-revocar", "hash-bcrypt", usuarioSistema);
        assertThat(clave.getActiva()).isTrue();
        assertThat(clave.getFechaRevocacion()).isNull();

        clave.revocar();

        assertThat(clave.getActiva()).isFalse();
        assertThat(clave.getFechaRevocacion()).isNotNull();
    }
}
