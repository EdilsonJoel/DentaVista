package com.dentavista.servicio;

import com.dentavista.IntegracionBase;
import com.dentavista.modelo.Cita;
import com.dentavista.modelo.EstadoCita;
import com.dentavista.modelo.MedioPago;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.dentavista.dominio.EstadoDental.CARIES;
import static com.dentavista.dominio.EstadoDental.OBTURADO;
import static com.dentavista.dominio.Superficie.OCLUSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dar de baja (archivar) a pacientes que ya no necesitan atención.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class ArchivoPacienteTest extends IntegracionBase {

    @Autowired private PacienteService servicio;
    @Autowired private PagoService pagos;
    @Autowired private ClinicaService clinica;
    @Autowired private PlanService planes;
    @Autowired private CitaService citas;

    private Usuario doctor;
    private Paciente paciente;

    @BeforeEach
    void preparar() {
        doctor = iniciarSesion(Rol.ODONTOLOGO);
        tratamiento("OBT", "90", OBTURADO, true);
        paciente = pacienteNuevo();
        // Plan de una obturación, ya realizada: debe S/ 90.
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        planes.generarDesdeOdontograma(paciente);
        planes.realizar(planes.items(paciente).get(0));
    }

    @Test
    void noSeArchivaAQuienTodaviaDebe() {
        assertThat(servicio.paraDarDeAlta()).doesNotContain(paciente);
        assertThatThrownBy(() -> servicio.archivar(paciente))
                .isInstanceOf(NegocioException.class).hasMessageContaining("debe S/ 90.00");
    }

    @Test
    void quienTerminoYPagoApareceParaDarDeAltaYSePuedeArchivarYReactivar() {
        pagos.registrar(paciente, new BigDecimal("90"), MedioPago.EFECTIVO, null);
        assertThat(servicio.paraDarDeAlta()).contains(paciente);

        servicio.archivar(paciente);
        assertThat(servicio.buscar("")).doesNotContain(paciente);
        assertThat(servicio.archivados("")).contains(paciente);
        assertThat(servicio.paraDarDeAlta()).doesNotContain(paciente);

        servicio.reactivar(paciente);
        assertThat(servicio.buscar("")).contains(paciente);
        assertThat(servicio.archivados("")).doesNotContain(paciente);
    }

    @Test
    void conCitaPorVenirNoEstaParaAltaYAlArchivarseCancela() {
        pagos.registrar(paciente, new BigDecimal("90"), MedioPago.EFECTIVO, null);
        Cita control = citas.agendar(paciente, doctor,
                LocalDate.now().plusWeeks(1).with(DayOfWeek.TUESDAY).atTime(9, 0), 30, "Control", null);
        assertThat(servicio.paraDarDeAlta()).doesNotContain(paciente);

        servicio.archivar(paciente);
        assertThat(citas.dePaciente(paciente)).extracting(Cita::getEstado).containsOnly(EstadoCita.CANCELADA);
        assertThat(control.getId()).isNotNull();
    }
}
