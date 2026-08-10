package com.ares.backend.service;

import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.exception.UnidadesIncompatiblesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de {@link ConversionService}.
 * No requiere contexto de Spring: las unidades se instancian directamente
 * como entidades sueltas (sin persistencia), siguiendo la convención del
 * resto de tests de servicio del proyecto (MockitoExtension + AssertJ).
 */
@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    private final ConversionService conversionService = new ConversionService();

    private UnidadMedida unidad(String codigo, TipoUnidad tipo, Double factorABase) {
        UnidadMedida u = new UnidadMedida();
        u.setId((long) codigo.hashCode());
        u.setCodigo(codigo);
        u.setNombre(codigo);
        u.setTipo(tipo);
        u.setFactorABase(factorABase);
        return u;
    }

    @Test
    @DisplayName("Convertir a la misma unidad devuelve el valor exacto sin ruido de coma flotante")
    void mismaUnidadDevuelveValorExacto() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);

        Double resultado = conversionService.convertir(2.0, kg, kg);

        assertThat(resultado).isEqualTo(2.0);
    }

    @Test
    @DisplayName("kg a g multiplica por el factor relativo (2kg -> 2000g)")
    void kgAGramos() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);
        UnidadMedida g = unidad("g", TipoUnidad.MASA, 1.0);

        Double resultado = conversionService.convertir(2.0, kg, g);

        assertThat(resultado).isEqualTo(2000.0);
    }

    @Test
    @DisplayName("g a kg divide por el factor relativo (500g -> 0.5kg)")
    void gramosAKg() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);
        UnidadMedida g = unidad("g", TipoUnidad.MASA, 1.0);

        Double resultado = conversionService.convertir(500.0, g, kg);

        assertThat(resultado).isEqualTo(0.5);
    }

    @Test
    @DisplayName("L a ml multiplica por el factor relativo (1.5L -> 1500ml)")
    void litrosAMililitros() {
        UnidadMedida litro = unidad("L", TipoUnidad.VOLUMEN, 1000.0);
        UnidadMedida ml = unidad("ml", TipoUnidad.VOLUMEN, 1.0);

        Double resultado = conversionService.convertir(1.5, litro, ml);

        assertThat(resultado).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("ml a L divide por el factor relativo (1500ml -> 1.5L)")
    void mililitrosALitros() {
        UnidadMedida litro = unidad("L", TipoUnidad.VOLUMEN, 1000.0);
        UnidadMedida ml = unidad("ml", TipoUnidad.VOLUMEN, 1.0);

        Double resultado = conversionService.convertir(1500.0, ml, litro);

        assertThat(resultado).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Masa a volumen se rechaza con excepción de dominio, nunca con un resultado forzado")
    void masaAVolumenLanzaExcepcion() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);
        UnidadMedida ml = unidad("ml", TipoUnidad.VOLUMEN, 1.0);

        assertThatThrownBy(() -> conversionService.convertir(1.0, kg, ml))
                .isInstanceOf(UnidadesIncompatiblesException.class);
    }

    @Test
    @DisplayName("Unidad a masa se rechaza con excepción de dominio")
    void unidadAMasaLanzaExcepcion() {
        UnidadMedida ud = unidad("ud", TipoUnidad.UNIDAD, 1.0);
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);

        assertThatThrownBy(() -> conversionService.convertir(1.0, ud, kg))
                .isInstanceOf(UnidadesIncompatiblesException.class);
    }

    @Test
    @DisplayName("Valor nulo lanza excepción en lugar de propagar un NPE silencioso")
    void valorNuloLanzaExcepcion() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);

        assertThatThrownBy(() -> conversionService.convertir(null, kg, kg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Unidad origen o destino nula lanza excepción")
    void unidadNulaLanzaExcepcion() {
        UnidadMedida kg = unidad("kg", TipoUnidad.MASA, 1000.0);

        assertThatThrownBy(() -> conversionService.convertir(1.0, null, kg))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conversionService.convertir(1.0, kg, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("El algoritmo es genérico: una unidad UNIDAD ad-hoc (docena, factor 12) convierte sin código especial")
    void unidadAdHocDocenaEsGenerica() {
        UnidadMedida ud = unidad("ud", TipoUnidad.UNIDAD, 1.0);
        UnidadMedida docena = unidad("docena", TipoUnidad.UNIDAD, 12.0);

        Double resultado = conversionService.convertir(1.0, docena, ud);

        assertThat(resultado).isEqualTo(12.0);
    }
}
