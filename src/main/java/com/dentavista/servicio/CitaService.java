package com.dentavista.servicio;

import com.dentavista.modelo.Cita;
import com.dentavista.modelo.EstadoCita;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.CitaRepository;
import com.dentavista.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agenda del consultorio.
 * <ul>
 *   <li>Horario de atención: lunes a viernes 8:00–13:00 y 15:00–19:00; sábados 8:00–13:00.</li>
 *   <li>Un odontólogo no puede tener dos citas a la vez, ni un paciente dos citas que se crucen.</li>
 *   <li>El <b>personal</b> agenda citas confirmadas; el <b>paciente</b> las solicita y la
 *       recepción las confirma.</li>
 * </ul>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CitaService {

    public static final int MINUTOS_BLOQUE = 30;
    private static final LocalTime[][] TURNOS_SEMANA = {
            {LocalTime.of(8, 0), LocalTime.of(13, 0)}, {LocalTime.of(15, 0), LocalTime.of(19, 0)}};
    private static final LocalTime[][] TURNOS_SABADO = {{LocalTime.of(8, 0), LocalTime.of(13, 0)}};

    private final CitaRepository citas;
    private final UsuarioRepository usuarios;
    private final com.dentavista.repositorio.ItemPlanRepository items;
    private final com.dentavista.repositorio.PacienteRepository pacientesRepo;
    private final SesionService sesion;

    // --------------------------------------------------------------- lectura

    @Transactional(readOnly = true)
    public List<Usuario> odontologos() {
        return usuarios.findByRolAndActivoTrueOrderByNombreAsc(Rol.ODONTOLOGO);
    }

    @Transactional(readOnly = true)
    public List<Cita> entre(LocalDate desde, LocalDate hastaInclusive) {
        sesion.exigirPersonal();
        return citas.findByInicioBetweenOrderByInicioAsc(desde.atStartOfDay(), hastaInclusive.plusDays(1).atStartOfDay().minusNanos(1));
    }

    @Transactional(readOnly = true)
    public List<Cita> delDia(LocalDate dia) {
        return entre(dia, dia);
    }

    @Transactional(readOnly = true)
    public List<Cita> dePaciente(Paciente p) {
        sesion.exigirAccesoA(p);
        return citas.findByPacienteOrderByInicioDesc(p);
    }

    @Transactional(readOnly = true)
    public Optional<Cita> proxima(Paciente p) {
        sesion.exigirAccesoA(p);
        LocalDateTime ahora = LocalDateTime.now();
        return citas.findByPacienteOrderByInicioDesc(p).stream()
                .filter(c -> c.getEstado().abierta() && c.getInicio().isAfter(ahora))
                .reduce((a, b) -> b);
    }

    /** Paciente con tratamientos pendientes y sin cita próxima (hay que llamarlo). */
    public record PorAgendar(Paciente paciente, long tratamientos, java.math.BigDecimal monto) {
    }

    @Transactional(readOnly = true)
    public List<PorAgendar> pacientesPorAgendar() {
        sesion.exigirPersonal();
        LocalDateTime ahora = LocalDateTime.now();
        java.util.Set<Long> conCita = new java.util.HashSet<>();
        citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(1)).stream()
                .filter(c -> c.getEstado().abierta())
                .forEach(c -> conCita.add(c.getPaciente().getId()));
        List<PorAgendar> r = new ArrayList<>();
        for (Object[] f : items.pendientesPorPaciente()) {
            long id = ((Number) f[0]).longValue();
            if (!conCita.contains(id)) {
                pacientesRepo.findById(id).filter(Paciente::isActivo).ifPresent(p ->
                        r.add(new PorAgendar(p, ((Number) f[1]).longValue(), (java.math.BigDecimal) f[2])));
            }
        }
        r.sort(java.util.Comparator.comparing(PorAgendar::monto).reversed());
        return r;
    }

    /** Solicitudes de pacientes pendientes de confirmar, la más próxima primero. */
    @Transactional(readOnly = true)
    public List<Cita> solicitudes() {
        sesion.exigirPersonal();
        LocalDateTime ahora = LocalDateTime.now();
        return citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(1)).stream()
                .filter(c -> c.getEstado() == EstadoCita.SOLICITADA).toList();
    }

    @Transactional(readOnly = true)
    public long porConfirmar() {
        return citas.countByEstado(EstadoCita.SOLICITADA);
    }

    /** Turnos de atención de un día (vacío si el consultorio no abre). */
    public static List<LocalTime[]> turnos(LocalDate dia) {
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }
        return List.of(dia.getDayOfWeek() == DayOfWeek.SATURDAY ? TURNOS_SABADO : TURNOS_SEMANA);
    }

    public static boolean dentroDelHorario(LocalDateTime inicio, int duracion) {
        LocalTime i = inicio.toLocalTime();
        LocalTime f = i.plusMinutes(duracion);
        return turnos(inicio.toLocalDate()).stream()
                .anyMatch(t -> !i.isBefore(t[0]) && !f.isAfter(t[1]) && f.isAfter(i));
    }

    /** Horas de inicio libres para un odontólogo en un día y una duración. */
    @Transactional(readOnly = true)
    public List<LocalTime> horariosLibres(Usuario odontologo, LocalDate dia, int duracion) {
        return horariosLibres(odontologo, dia, duracion, null);
    }

    /** Igual, pero sin contar la cita que se está reprogramando (su propio horario cuenta como libre). */
    @Transactional(readOnly = true)
    public List<LocalTime> horariosLibres(Usuario odontologo, LocalDate dia, int duracion, Cita excluida) {
        List<Cita> ocupadas = citas.findByOdontologoAndInicioBetweenOrderByInicioAsc(odontologo,
                        dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()).stream()
                .filter(c -> c.getEstado().ocupaHorario() && !esLaMisma(c, excluida)).toList();
        LocalDateTime minimo = LocalDateTime.now().plusMinutes(30);
        List<LocalTime> libres = new ArrayList<>();
        for (LocalTime[] t : turnos(dia)) {
            for (LocalTime h = t[0]; !h.plusMinutes(duracion).isAfter(t[1]); h = h.plusMinutes(MINUTOS_BLOQUE)) {
                LocalDateTime i = dia.atTime(h);
                LocalDateTime f = i.plusMinutes(duracion);
                if (i.isAfter(minimo) && ocupadas.stream().noneMatch(c -> c.seCruzaCon(i, f))) {
                    libres.add(h);
                }
            }
        }
        return libres;
    }

    // ----------------------------------------------------------- operaciones

    /**
     * Agenda (personal) o solicita (paciente) una cita.
     */
    public Cita agendar(Paciente p, Usuario odontologo, LocalDateTime inicio, int duracion, String motivo, ItemPlan item) {
        if (sesion.esPaciente()) {
            sesion.exigirAccesoA(p);
        } else {
            sesion.exigirPersonal();
        }
        if (p == null) {
            throw new NegocioException("Elige el paciente.");
        }
        validarHorario(p, odontologo, inicio, duracion, null);
        return citas.save(Cita.builder()
                .paciente(p)
                .odontologo(odontologo)
                .inicio(inicio)
                .duracion(duracion)
                .motivo(motivo == null || motivo.isBlank() ? null : motivo.trim())
                .itemPlan(item)
                .estado(sesion.esPaciente() ? EstadoCita.SOLICITADA : EstadoCita.CONFIRMADA)
                .creadaPor(sesion.nombre())
                .creadaEn(LocalDateTime.now())
                .build());
    }

    /**
     * Cambia fecha, hora, duración u odontólogo de una cita abierta. Si la reprograma el
     * paciente vuelve a quedar "por confirmar"; si lo hace el personal queda confirmada.
     */
    public Cita reprogramar(Cita c, Usuario odontologo, LocalDateTime inicio, int duracion) {
        Cita actual = obtener(c);
        exigirPuedeModificar(actual);
        validarHorario(actual.getPaciente(), odontologo, inicio, duracion, actual);
        actual.setOdontologo(odontologo);
        actual.setInicio(inicio);
        actual.setDuracion(duracion);
        actual.setEstado(sesion.esPaciente() ? EstadoCita.SOLICITADA : EstadoCita.CONFIRMADA);
        return citas.save(actual);
    }

    /** Fecha futura, dentro del horario y sin cruces para el odontólogo ni para el paciente. */
    private void validarHorario(Paciente p, Usuario odontologo, LocalDateTime inicio, int duracion, Cita excluida) {
        if (odontologo == null) {
            throw new NegocioException("Elige el odontólogo.");
        }
        if (inicio == null) {
            throw new NegocioException("Elige la fecha y la hora de la cita.");
        }
        if (duracion != 30 && duracion != 60 && duracion != 90) {
            throw new NegocioException("La duración debe ser 30, 60 o 90 minutos.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new NegocioException("No se puede agendar una cita en el pasado.");
        }
        if (!dentroDelHorario(inicio, duracion)) {
            throw new NegocioException("Fuera del horario de atención (lunes a viernes 8:00–13:00 y 15:00–19:00, "
                    + "sábados 8:00–13:00).");
        }
        LocalDateTime fin = inicio.plusMinutes(duracion);
        citas.findByOdontologoAndInicioBetweenOrderByInicioAsc(odontologo, inicio.toLocalDate().atStartOfDay(),
                        inicio.toLocalDate().plusDays(1).atStartOfDay()).stream()
                .filter(c -> c.getEstado().ocupaHorario() && !esLaMisma(c, excluida) && c.seCruzaCon(inicio, fin))
                .findFirst()
                .ifPresent(c -> {
                    throw new NegocioException(odontologo.getNombre() + " ya tiene una cita de " + c.getInicio().toLocalTime()
                            + " a " + c.fin().toLocalTime() + ". Elige otro horario.");
                });
        citas.findByPacienteAndInicioBetween(p, inicio.toLocalDate().atStartOfDay(), inicio.toLocalDate().plusDays(1).atStartOfDay())
                .stream()
                .filter(c -> c.getEstado().ocupaHorario() && !esLaMisma(c, excluida) && c.seCruzaCon(inicio, fin))
                .findFirst()
                .ifPresent(c -> {
                    throw new NegocioException(p.nombreCompleto() + " ya tiene una cita a esa hora ("
                            + c.getInicio().toLocalTime() + ").");
                });
    }

    private static boolean esLaMisma(Cita c, Cita otra) {
        return otra != null && c.getId().equals(otra.getId());
    }

    /** El personal modifica cualquier cita abierta; el paciente, solo las suyas futuras. */
    private void exigirPuedeModificar(Cita actual) {
        if (sesion.esPaciente()) {
            sesion.exigirAccesoA(actual.getPaciente());
            if (actual.getInicio().isBefore(LocalDateTime.now())) {
                throw new NegocioException("La cita ya pasó.");
            }
        } else {
            sesion.exigirPersonal();
        }
        if (!actual.getEstado().abierta()) {
            throw new NegocioException("La cita ya está " + actual.getEstado().getEtiqueta().toLowerCase() + ".");
        }
    }

    public Cita confirmar(Cita c) {
        sesion.exigirPersonal();
        Cita actual = obtener(c);
        if (actual.getEstado() != EstadoCita.SOLICITADA) {
            throw new NegocioException("Solo se confirman citas solicitadas por el paciente.");
        }
        actual.setEstado(EstadoCita.CONFIRMADA);
        return citas.save(actual);
    }

    /** El personal cancela cualquier cita abierta; el paciente, solo las suyas futuras. */
    public Cita cancelar(Cita c) {
        Cita actual = obtener(c);
        exigirPuedeModificar(actual);
        actual.setEstado(EstadoCita.CANCELADA);
        return citas.save(actual);
    }

    public Cita marcarAtendida(Cita c) {
        sesion.exigirPersonal();
        Cita actual = obtener(c);
        if (!actual.getEstado().abierta()) {
            throw new NegocioException("La cita ya está " + actual.getEstado().getEtiqueta().toLowerCase() + ".");
        }
        actual.setEstado(EstadoCita.ATENDIDA);
        return citas.save(actual);
    }

    public Cita marcarNoAsistio(Cita c) {
        sesion.exigirPersonal();
        Cita actual = obtener(c);
        if (!actual.getEstado().abierta()) {
            throw new NegocioException("La cita ya está " + actual.getEstado().getEtiqueta().toLowerCase() + ".");
        }
        actual.setEstado(EstadoCita.NO_ASISTIO);
        return citas.save(actual);
    }

    /** Uso interno (datos de demostración): guarda sin validar horario ni fecha. */
    public Cita guardar(Cita c) {
        return citas.save(c);
    }

    private Cita obtener(Cita c) {
        return citas.findById(c.getId()).orElseThrow(() -> new NegocioException("La cita ya no existe."));
    }
}
