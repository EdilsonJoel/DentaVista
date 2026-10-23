package com.dentavista.servicio;

import com.dentavista.IntegracionBase;
import com.dentavista.modelo.Cita;
import com.dentavista.modelo.EstadoCita;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Agenda: horario, cruces de horario y permisos de pacientes con acceso propio.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class CitaServiceTest extends IntegracionBase {

    @Autowired private CitaService citas;
    @Autowired private UsuarioService usuariosService;
    @Autowired private ClinicaService clinica;

    private Usuario doctor;
    private Paciente paciente;
    /** Un martes futuro: siempre hay atención mañana y tarde. */
    private final LocalDate martes = LocalDate.now().plusWeeks(1).with(DayOfWeek.TUESDAY);

    @BeforeEach
    void preparar() {
        doctor = iniciarSesion(Rol.ODONTOLOGO);
        iniciarSesion(Rol.ASISTENTE);
        paciente = pacienteNuevo();
    }

    private LocalDateTime a(int h, int m) {
        return martes.atTime(h, m);
    }

    @Test
    void laRecepcionAgendaCitasConfirmadas() {
        Cita c = citas.agendar(paciente, doctor, a(9, 0), 30, "Control", null);
        assertThat(c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(c.fin()).isEqualTo(a(9, 30));
    }

    @Test
    void reprogramarMueveLaCitaYRespetaLosCruces() {
        Cita c = citas.agendar(paciente, doctor, a(9, 0), 60, "Control", null);
        // Puede moverse dentro de su propio horario (no choca consigo misma).
        Cita movida = citas.reprogramar(c, doctor, a(9, 30), 60);
        assertThat(movida.getInicio()).isEqualTo(a(9, 30));
        assertThat(citas.horariosLibres(doctor, martes, 60)).doesNotContain(LocalTime.of(9, 30));

        Cita otra = citas.agendar(pacienteNuevo(), doctor, a(11, 0), 30, null, null);
        assertThatThrownBy(() -> citas.reprogramar(otra, doctor, a(10, 0), 30))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya tiene una cita");

        citas.cancelar(otra);
        assertThatThrownBy(() -> citas.reprogramar(otra, doctor, a(12, 0), 30)).hasMessageContaining("cancelada");
    }

    @Test
    void unOdontologoNoPuedeTenerDosCitasCruzadas() {
        citas.agendar(paciente, doctor, a(9, 0), 60, null, null);
        Paciente otro = pacienteNuevo();
        assertThatThrownBy(() -> citas.agendar(otro, doctor, a(9, 30), 30, null, null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya tiene una cita");
        assertThat(citas.agendar(otro, doctor, a(10, 0), 30, null, null)).isNotNull();
    }

    @Test
    void respetaElHorarioDeAtencion() {
        assertThatThrownBy(() -> citas.agendar(paciente, doctor, a(13, 30), 30, null, null))
                .hasMessageContaining("Fuera del horario");
        assertThatThrownBy(() -> citas.agendar(paciente, doctor, a(12, 30), 60, null, null))
                .hasMessageContaining("Fuera del horario");
        LocalDateTime sabadoTarde = martes.with(DayOfWeek.SATURDAY).atTime(16, 0);
        assertThatThrownBy(() -> citas.agendar(paciente, doctor, sabadoTarde, 30, null, null))
                .hasMessageContaining("Fuera del horario");
        assertThatThrownBy(() -> citas.agendar(paciente, doctor, LocalDateTime.now().minusDays(1), 30, null, null))
                .hasMessageContaining("pasado");
    }

    @Test
    void losHorariosLibresExcluyenLasCitasYLaPausa() {
        citas.agendar(paciente, doctor, a(9, 0), 60, null, null);
        var libres = citas.horariosLibres(doctor, martes, 30);
        assertThat(libres).doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(13, 0), LocalTime.of(14, 0))
                .contains(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(15, 0), LocalTime.of(18, 30));
    }

    @Test
    void unaCitaCanceladaLiberaElHorario() {
        Cita c = citas.agendar(paciente, doctor, a(11, 0), 30, null, null);
        citas.cancelar(c);
        assertThat(citas.horariosLibres(doctor, martes, 30)).contains(LocalTime.of(11, 0));
        assertThatThrownBy(() -> citas.marcarAtendida(c)).hasMessageContaining("cancelada");
    }

    @Test
    void elPacienteSolicitaSusCitasYSoloVeSuFicha() {
        Usuario acceso = usuariosService.crearAccesoPaciente(paciente, null, "clave123");
        assertThat(acceso.getUsuario()).isEqualTo(paciente.getDni());
        sesion.cerrar();
        usuariosService.autenticar(paciente.getDni(), "clave123");

        Cita solicitada = citas.agendar(paciente, doctor, a(16, 0), 30, "Me duele una muela", null);
        assertThat(solicitada.getEstado()).isEqualTo(EstadoCita.SOLICITADA);
        assertThat(clinica.odontograma(paciente)).isNotNull();

        Paciente ajeno = pacientes.save(Paciente.builder().dni("87654321").nombres("Otro").apellidos("Paciente").build());
        assertThatThrownBy(() -> clinica.odontograma(ajeno)).hasMessageContaining("propia ficha");
        assertThatThrownBy(() -> citas.agendar(ajeno, doctor, a(17, 0), 30, null, null)).hasMessageContaining("propia ficha");
        assertThatThrownBy(() -> citas.confirmar(solicitada)).hasMessageContaining("personal");

        iniciarSesion(Rol.ASISTENTE);
        assertThat(citas.confirmar(solicitada).getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(citas.porConfirmar()).isZero();
    }
}
