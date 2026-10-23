package com.dentavista.tresd;

import com.dentavista.dominio.Pieza;
import com.dentavista.dominio.Superficie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El modelo OBJ se reconoce como una dentadura completa con sus caras clinicas.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class ModeloDentalTest {

    private static ModeloDental modelo;

    @BeforeAll
    static void cargar() {
        modelo = ModeloDental.get();
    }

    @Test
    void reconoceLas32PiezasFdiYDosEncias() {
        assertThat(modelo.dientes().keySet()).containsExactlyInAnyOrderElementsOf(Pieza.TODAS);
        assertThat(modelo.encias()).hasSize(2);
        assertThat(modelo.encias()).extracting(ModeloDental.Encia::superior).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void lasPiezasSuperioresEstanPorEncimaDeLasInferiores() {
        for (int pos = 1; pos <= 8; pos++) {
            assertThat(modelo.diente(10 + pos).centro()[1]).isGreaterThan(modelo.diente(40 + pos).centro()[1]);
            assertThat(modelo.diente(20 + pos).centro()[1]).isGreaterThan(modelo.diente(30 + pos).centro()[1]);
        }
    }

    @Test
    void elLadoDerechoDelPacienteQuedaEnXNegativo() {
        assertThat(modelo.diente(16).centro()[0]).isNegative();
        assertThat(modelo.diente(46).centro()[0]).isNegative();
        assertThat(modelo.diente(26).centro()[0]).isPositive();
        assertThat(modelo.diente(36).centro()[0]).isPositive();
    }

    @Test
    void cadaDienteTieneSusCincoCaras() {
        for (ModeloDental.Diente d : modelo.dientes().values()) {
            Map<Superficie, Integer> conteo = new EnumMap<>(Superficie.class);
            Arrays.stream(d.caraDeTriangulo()).forEach(s -> conteo.merge(s, 1, Integer::sum));
            assertThat(conteo.keySet())
                    .as("caras de la pieza %d", d.fdi())
                    .containsExactlyInAnyOrder(Superficie.CARAS);
            conteo.forEach((cara, n) -> assertThat(n)
                    .as("triangulos de la cara %s de la pieza %d", cara, d.fdi())
                    // El borde incisal de incisivos y caninos es estrecho por anatomia.
                    .isGreaterThan(cara == Superficie.OCLUSAL && Pieza.esAnterior(d.fdi()) ? 4 : 20));
        }
    }

    @Test
    void laSubdivisionSuavizaLaMalla() {
        // Cada paso multiplica por ~4 las caras: el diente deja de verse facetado.
        assertThat(modelo.diente(16).triangulos().size()).isGreaterThan(600);
    }
}
