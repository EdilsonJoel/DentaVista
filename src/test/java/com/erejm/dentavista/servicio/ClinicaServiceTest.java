package com.erejm.dentavista.servicio;

import com.erejm.dentavista.IntegracionBase;
import com.erejm.dentavista.modelo.Hallazgo;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.erejm.dentavista.dominio.EstadoDental.*;
import static com.erejm.dentavista.dominio.Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Registro de hallazgos: validaciones clínicas, consulta del día y deshacer.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class ClinicaServiceTest extends IntegracionBase {

    @Autowired
    private ClinicaService clinica;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        iniciarSesion(Rol.ODONTOLOGO);
        paciente = pacienteNuevo();
    }

    @Test
    void registraUnaCariesYAbreLaConsultaDelDia() {
        Hallazgo h = clinica.registrar(paciente, 16, OCLUSAL, CARIES, "Lesión profunda");
        assertThat(h.getConsulta()).isNotNull();
        assertThat(h.getNota()).isEqualTo("Lesión profunda");
        assertThat(clinica.odontograma(paciente).pieza(16).cara(OCLUSAL)).isEqualTo(CARIES);

        clinica.registrar(paciente, 26, MESIAL, CARIES, null);
        assertThat(clinica.consultas(paciente)).hasSize(1);
    }

    @Test
    void repetirElMismoEstadoNoCreaRegistros() {
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        assertThat(clinica.registrar(paciente, 16, OCLUSAL, CARIES, null)).isNull();
        assertThat(clinica.hallazgos(paciente)).hasSize(1);
    }

    @Test
    void unaCariesNoSePuedeRegistrarEnLaPiezaCompleta() {
        assertThatThrownBy(() -> clinica.registrar(paciente, 16, PIEZA, CARIES, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("cara");
    }

    @Test
    void unaCoronaNoSeRegistraEnUnaSolaCara() {
        assertThatThrownBy(() -> clinica.registrar(paciente, 11, VESTIBULAR, CORONA, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pieza completa");
    }

    @Test
    void unaPiezaAusenteNoTieneCaras() {
        clinica.registrar(paciente, 36, PIEZA, AUSENTE, null);
        assertThatThrownBy(() -> clinica.registrar(paciente, 36, OCLUSAL, CARIES, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("36");
    }

    @Test
    void deshacerEliminaElUltimoRegistroDeHoy() {
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.registrar(paciente, 17, OCLUSAL, CARIES, null);
        Hallazgo deshecho = clinica.deshacerUltimo(paciente);
        assertThat(deshecho.getPieza()).isEqualTo(17);
        assertThat(clinica.odontograma(paciente).pieza(17).sana()).isTrue();
        assertThat(clinica.odontograma(paciente).pieza(16).cara(OCLUSAL)).isEqualTo(CARIES);
    }

    @Test
    void deshacerElUnicoRegistroEliminaLaConsultaVacia() {
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.deshacerUltimo(paciente);
        assertThat(clinica.consultas(paciente)).isEmpty();
        assertThat(clinica.deshacerUltimo(paciente)).isNull();
    }

    @Test
    void laRecepcionNoPuedeModificarLaHistoriaClinica() {
        iniciarSesion(Rol.ASISTENTE);
        assertThatThrownBy(() -> clinica.registrar(paciente, 16, OCLUSAL, CARIES, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no permite");
    }
}
