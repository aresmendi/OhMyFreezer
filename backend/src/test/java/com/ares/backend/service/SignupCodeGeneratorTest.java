package com.ares.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link SignupCodeGenerator}. No requiere contexto de
 * Spring: se instancia directamente, siguiendo la convención de {@code
 * ConversionServiceTest} (MockitoExtension sin mocks necesarios).
 */
@ExtendWith(MockitoExtension.class)
class SignupCodeGeneratorTest {

    private static final String ALFABETO_ESPERADO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CARACTERES_AMBIGUOS = "IO01";

    private final SignupCodeGenerator generator = new SignupCodeGenerator();

    @Test
    @DisplayName("genera un código de longitud exactamente 10")
    void generaCodigoDeLongitud10() {
        String codigo = generator.generar();

        assertThat(codigo).hasSize(10);
    }

    @Test
    @DisplayName("el código generado usa exclusivamente símbolos del alfabeto de 32 caracteres")
    void codigoUsaSoloAlfabetoPermitido() {
        String codigo = generator.generar();

        assertThat(codigo).matches("[" + ALFABETO_ESPERADO + "]{10}");
    }

    @Test
    @DisplayName("el código generado nunca contiene los caracteres ambiguos I, O, 0, 1")
    void codigoNuncaContieneCaracteresAmbiguos() {
        // Generamos muchos códigos para dar a cada símbolo del alfabeto
        // oportunidad estadística real de aparecer si el filtro fallara.
        for (int i = 0; i < 1000; i++) {
            String codigo = generator.generar();
            for (char c : CARACTERES_AMBIGUOS.toCharArray()) {
                assertThat(codigo).as("código '%s' no debe contener '%s'", codigo, c).doesNotContain(String.valueOf(c));
            }
        }
    }

    @Test
    @DisplayName("10.000 códigos generados son todos distintos entre sí (entropía real, no un valor fijo)")
    void generaCodigosDistintosEntreSi() {
        Set<String> codigos = new HashSet<>();
        IntStream.range(0, 10_000).forEach(i -> codigos.add(generator.generar()));

        assertThat(codigos).hasSize(10_000);
    }
}
