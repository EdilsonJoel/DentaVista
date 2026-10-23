package com.dentavista.servicio;

import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Paciente;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
@Service
@RequiredArgsConstructor
@Transactional
public class PacienteService {

    private final PacienteRepository pacientes;
    private final ConsultaRepository consultas;
    private final PagoService pagos;
    private final com.dentavista.repositorio.ItemPlanRepository items;
    private final com.dentavista.repositorio.CitaRepository citas;
    private final com.dentavista.repositorio.UsuarioRepository usuarios;
    private final Validacion validacion;
    private final SesionService sesion;

    @Transactional(readOnly = true)
    public List<Paciente> buscar(String texto) {
        sesion.exigirPersonal();
        return pacientes.buscar(texto == null ? "" : texto.trim(), true);
    }

    /** Pacientes dados de baja (archivados). */
    @Transactional(readOnly = true)
    public List<Paciente> archivados(String texto) {
        sesion.exigirPersonal();
        return pacientes.buscar(texto == null ? "" : texto.trim(), false);
    }

    /**
     * Pacientes que ya no necesitan nada: terminaron todo su plan, lo pagaron y no
     * tienen citas por venir. Son los candidatos a archivar.
     */
    @Transactional(readOnly = true)
    public List<Paciente> paraDarDeAlta() {
        sesion.exigirPersonal();
        java.util.Set<Long> conPendientes = new java.util.HashSet<>();
        items.pendientesPorPaciente().forEach(f -> conPendientes.add(((Number) f[0]).longValue()));
        LocalDateTime ahora = LocalDateTime.now();
        java.util.Set<Long> conCita = new java.util.HashSet<>();
        citas.findByInicioBetweenOrderByInicioAsc(ahora, ahora.plusYears(2)).stream()
                .filter(c -> c.getEstado().abierta())
                .forEach(c -> conCita.add(c.getPaciente().getId()));
        return pagos.saldos().entrySet().stream()
                .filter(e -> e.getValue().signum() <= 0 && !conPendientes.contains(e.getKey()) && !conCita.contains(e.getKey()))
                .map(e -> pacientes.findById(e.getKey()).orElse(null))
                .filter(p -> p != null && p.isActivo())
                .sorted(java.util.Comparator.comparing(Paciente::getApellidos).thenComparing(Paciente::getNombres))
                .toList();
    }

    @Transactional(readOnly = true)
    public Paciente obtener(Long id) {
        Paciente p = pacientes.findById(id)
                .orElseThrow(() -> new NegocioException("El paciente ya no existe."));
        sesion.exigirAccesoA(p);
        return p;
    }

    public Paciente guardar(Paciente p) {
        if (sesion.haySesion()) {
            sesion.exigirPersonal();
        }
        p.setDni(p.getDni() == null ? null : p.getDni().trim());
        p.setNombres(limpiar(p.getNombres()));
        p.setApellidos(limpiar(p.getApellidos()));
        validacion.validar(p);
        pacientes.findByDni(p.getDni())
                .filter(otro -> !otro.getId().equals(p.getId()))
                .ifPresent(otro -> {
                    throw new NegocioException("Ya existe un paciente con DNI " + p.getDni()
                            + ": " + otro.nombreCompleto() + ".");
                });
        return pacientes.save(p);
    }

    /**
     * Da de baja a un paciente que ya no necesita atención. Es una baja lógica: su
     * historia clínica se conserva (obligación legal) y puede reactivarse. Se cancelan
     * sus citas por venir y se desactiva su acceso. No se archiva a quien aún debe.
     */
    public Paciente archivar(Paciente p) {
        sesion.exigirPersonal();
        Paciente actual = obtener(p.getId());
        java.math.BigDecimal deuda = pagos.cuenta(actual).deuda();
        if (deuda.signum() > 0) {
            throw new NegocioException(actual.nombreCompleto() + " debe S/ " + deuda.setScale(2, java.math.RoundingMode.HALF_UP)
                    + " por tratamientos realizados. Registra el pago antes de archivarlo.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        citas.findByPacienteOrderByInicioDesc(actual).stream()
                .filter(c -> c.getEstado().abierta() && c.getInicio().isAfter(ahora))
                .forEach(c -> {
                    c.setEstado(com.dentavista.modelo.EstadoCita.CANCELADA);
                    citas.save(c);
                });
        usuarios.findByPaciente(actual).ifPresent(u -> {
            u.setActivo(false);
            usuarios.save(u);
        });
        actual.setActivo(false);
        return pacientes.save(actual);
    }

    /** Vuelve a activar a un paciente archivado (y su acceso, si tenía). */
    public Paciente reactivar(Paciente p) {
        sesion.exigirPersonal();
        Paciente actual = obtener(p.getId());
        usuarios.findByPaciente(actual).ifPresent(u -> {
            u.setActivo(true);
            usuarios.save(u);
        });
        actual.setActivo(true);
        return pacientes.save(actual);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDateTime> ultimaVisita(Paciente p) {
        return consultas.findFirstByPacienteOrderByFechaDesc(p).map(Consulta::getFecha);
    }

    private static String limpiar(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }
}
