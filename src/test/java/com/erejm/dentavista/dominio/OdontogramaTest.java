package com.erejm.dentavista.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.erejm.dentavista.dominio.EstadoDental.*;
import static com.erejm.dentavista.dominio.Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reglas de reconstrucción del odontograma a partir de los registros clínicos.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class OdontogramaTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 10, 9, 0);
    private final List<Odontograma.Registro> registros = new ArrayList<>();
    private long secuencia;

    private void registrar(int dias, int pieza, Superficie s, EstadoDental e) {
        registros.add(new Odontograma.Registro(pieza, s, e, T0.plusDays(dias), secuencia++));
    }

    @Test
    void sinRegistrosLas32PiezasEstanSanas() {
        Odontograma o = Odontograma.vacio();
        assertThat(o.piezas()).hasSize(32).allMatch(Odontograma.EstadoPieza::sana);
        assertThat(o.patologias()).isEmpty();
        assertThat(o.piezasPresentes()).isEqualTo(32);
    }

    @Test
    void unaCariesSoloAfectaASuCara() {
        registrar(0, 16, OCLUSAL, CARIES);
        Odontograma.EstadoPieza p = Odontograma.reconstruir(registros).pieza(16);
        assertThat(p.visible(OCLUSAL)).isEqualTo(CARIES);
        assertThat(p.visible(MESIAL)).isEqualTo(SANO);
        assertThat(p.descripcion()).isEqualTo("Caries O");
    }

    @Test
    void laObturacionPosteriorReemplazaALaCaries() {
        registrar(0, 16, OCLUSAL, CARIES);
        registrar(30, 16, OCLUSAL, OBTURADO);
        Odontograma o = Odontograma.reconstruir(registros);
        assertThat(o.pieza(16).cara(OCLUSAL)).isEqualTo(OBTURADO);
        assertThat(o.patologias()).isEmpty();
    }

    @Test
    void laLineaDeTiempoMuestraLaBocaEnUnaFechaPasada() {
        registrar(0, 16, OCLUSAL, CARIES);
        registrar(30, 16, OCLUSAL, OBTURADO);
        Odontograma antes = Odontograma.reconstruir(registros, T0.plusDays(10));
        assertThat(antes.pieza(16).cara(OCLUSAL)).isEqualTo(CARIES);
        assertThat(antes.patologias()).extracting(Odontograma.Patologia::pieza).containsExactly(16);
    }

    @Test
    void ausenteBorraLasCarasYPintaTodaLaPieza() {
        registrar(0, 36, OCLUSAL, CARIES);
        registrar(1, 36, PIEZA, AUSENTE);
        Odontograma.EstadoPieza p = Odontograma.reconstruir(registros).pieza(36);
        assertThat(p.ausente()).isTrue();
        assertThat(p.caras()).isEmpty();
        assertThat(p.visible(OCLUSAL)).isEqualTo(AUSENTE);
    }

    @Test
    void laEndodonciaConservaLasCarasPropias() {
        registrar(0, 46, MESIAL, CARIES);
        registrar(1, 46, PIEZA, ENDODONCIA);
        Odontograma.EstadoPieza p = Odontograma.reconstruir(registros).pieza(46);
        assertThat(p.visible(MESIAL)).isEqualTo(CARIES);
        assertThat(p.visible(OCLUSAL)).isEqualTo(ENDODONCIA);
    }

    @Test
    void sanoSobreLaPiezaLaRestableceCompleta() {
        registrar(0, 11, VESTIBULAR, FRACTURA);
        registrar(0, 11, PIEZA, CORONA);
        registrar(1, 11, PIEZA, SANO);
        assertThat(Odontograma.reconstruir(registros).pieza(11).sana()).isTrue();
    }

    @Test
    void elOrdenEsCronologicoAunqueLosRegistrosLleguenDesordenados() {
        registros.add(new Odontograma.Registro(21, OCLUSAL, OBTURADO, T0.plusDays(5), 1));
        registros.add(new Odontograma.Registro(21, OCLUSAL, CARIES, T0, 2));
        assertThat(Odontograma.reconstruir(registros).pieza(21).cara(OCLUSAL)).isEqualTo(OBTURADO);
    }

    @Test
    void lasPatologiasIndicanElTratamientoSugerido() {
        registrar(0, 16, OCLUSAL, CARIES);
        registrar(0, 48, PIEZA, EXTRACCION_INDICADA);
        registrar(0, 26, PIEZA, ENDODONCIA_INDICADA);
        assertThat(Odontograma.reconstruir(registros).patologias())
                .extracting(pat -> pat.estado().getTratamientoSugerido())
                .containsExactlyInAnyOrder("OBT", "EXO", "END");
    }

    @Test
    void rechazaPiezasQueNoExistenEnFdi() {
        assertThatThrownBy(() -> registrar(0, 19, OCLUSAL, CARIES)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar(0, 51, OCLUSAL, CARIES)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nombresClinicosSegunLaPieza() {
        assertThat(Pieza.nombre(16)).isEqualTo("Primer molar superior derecho");
        assertThat(Pieza.nombre(38)).isEqualTo("Tercer molar inferior izquierdo");
        assertThat(OCLUSAL.etiqueta(11)).isEqualTo("Incisal");
        assertThat(LINGUAL.etiqueta(26)).isEqualTo("Palatina");
        assertThat(LINGUAL.etiqueta(36)).isEqualTo("Lingual");
        assertThat(Pieza.espejo(16)).isEqualTo(26);
    }
}
