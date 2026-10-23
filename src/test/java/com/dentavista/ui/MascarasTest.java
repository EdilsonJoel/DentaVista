package com.dentavista.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las máscaras completan el formato y descartan lo inválido.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class MascarasTest {

    @Test
    void laFechaSeArmaSoloConLosDigitos() {
        assertThat(Mascaras.conBarras("12", false)).isEqualTo("12/");
        assertThat(Mascaras.conBarras("1205", false)).isEqualTo("12/05/");
        assertThat(Mascaras.conBarras("12051996", false)).isEqualTo("12/05/1996");
        assertThat(Mascaras.conBarras("12", true)).isEqualTo("12");
    }

    @Test
    void interpretaFechasCompletasYConAnioCorto() {
        assertThat(Mascaras.interpretar("12/05/1996")).isEqualTo(LocalDate.of(1996, 5, 12));
        assertThat(Mascaras.interpretar("12051996")).isEqualTo(LocalDate.of(1996, 5, 12));
        assertThat(Mascaras.interpretar("120596")).isEqualTo(LocalDate.of(1996, 5, 12));
        int yy = LocalDate.now().getYear() % 100;
        assertThat(Mascaras.interpretar("0101" + String.format("%02d", yy)).getYear()).isEqualTo(2000 + yy);
        assertThat(Mascaras.interpretar("31/02/2020")).isNull();
        assertThat(Mascaras.interpretar("12/05")).isNull();
    }

    @Test
    void capitalizaNombresRespetandoConectores() {
        assertThat(Mascaras.capitalizar("  valeria   QUISPE huanca ")).isEqualTo("Valeria Quispe Huanca");
        assertThat(Mascaras.capitalizar("maría DE LA cruz")).isEqualTo("María de la Cruz");
        assertThat(Mascaras.capitalizar("dra. lucía paredes")).isEqualTo("Dra. Lucía Paredes");
    }
}
