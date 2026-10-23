package com.dentavista.servicio;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.dominio.Odontograma;
import com.dentavista.dominio.Pieza;
import com.dentavista.dominio.Superficie;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Hallazgo;
import com.dentavista.modelo.Paciente;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.HallazgoRepository;
import com.dentavista.repositorio.ItemPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Historia clínica odontológica: consultas y hallazgos sobre el odontograma.
 * <p>
 * Cada clic en el odontograma (3D o 2D) termina en {@link #registrar}: valida que
 * el hallazgo tenga sentido clínico, lo guarda en la consulta del día y devuelve el
 * registro. El odontograma nunca se guarda "pintado": se reconstruye con
 * {@link #odontograma(Paciente, LocalDateTime)}.
 * </p>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClinicaService {

    private final HallazgoRepository hallazgos;
    private final ConsultaRepository consultas;
    private final ItemPlanRepository items;
    private final SesionService sesion;

    // --------------------------------------------------------------- lectura

    @Transactional(readOnly = true)
    public List<Hallazgo> hallazgos(Paciente p) {
        sesion.exigirAccesoA(p);
        return hallazgos.findByPacienteOrderByFechaAscIdAsc(p);
    }

    /** Boca del paciente en una fecha (null = estado actual). */
    @Transactional(readOnly = true)
    public Odontograma odontograma(Paciente p, LocalDateTime hasta) {
        return Odontograma.reconstruir(hallazgos(p).stream().map(Hallazgo::aRegistro).toList(), hasta);
    }

    @Transactional(readOnly = true)
    public Odontograma odontograma(Paciente p) {
        return odontograma(p, null);
    }

    @Transactional(readOnly = true)
    public List<Hallazgo> historialPieza(Paciente p, int fdi) {
        sesion.exigirAccesoA(p);
        return hallazgos.findByPacienteAndPiezaOrderByFechaDescIdDesc(p, fdi);
    }

    @Transactional(readOnly = true)
    public List<Consulta> consultas(Paciente p) {
        sesion.exigirAccesoA(p);
        return consultas.findByPacienteOrderByFechaDesc(p);
    }

    @Transactional(readOnly = true)
    public List<Consulta> consultasCronologicas(Paciente p) {
        sesion.exigirAccesoA(p);
        return consultas.findByPacienteOrderByFechaAsc(p);
    }

    @Transactional(readOnly = true)
    public List<Hallazgo> hallazgosDe(Consulta c) {
        return hallazgos.findByConsultaOrderByIdAsc(c);
    }

    // ------------------------------------------------------------- consultas

    /** La consulta de hoy del paciente; se abre automáticamente con el primer registro. */
    public Consulta consultaDeHoy(Paciente p) {
        LocalDate hoy = LocalDate.now();
        return consultas.findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
                        p, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1))
                .orElseGet(() -> consultas.save(Consulta.builder()
                        .paciente(p)
                        .odontologo(sesion.getUsuario())
                        .fecha(LocalDateTime.now())
                        .motivo("Consulta odontológica")
                        .build()));
    }

    // -------------------------------------------------------------- registro

    /**
     * Registra un hallazgo hecho por el usuario en la consulta de hoy.
     *
     * @return el hallazgo guardado, o null si no cambia nada (la cara ya tenía ese estado)
     */
    public Hallazgo registrar(Paciente p, int pieza, Superficie superficie, EstadoDental estado, String nota) {
        sesion.exigirClinica();
        validar(p, pieza, superficie, estado);
        Odontograma.EstadoPieza actual = odontograma(p).pieza(pieza);
        if (sinCambio(actual, superficie, estado)) {
            return null;
        }
        return registrarEn(p, consultaDeHoy(p), LocalDateTime.now(), pieza, superficie, estado, nota);
    }

    /** Registro sin comprobaciones de interfaz (datos de demostración y tratamientos realizados). */
    public Hallazgo registrarEn(Paciente p, Consulta consulta, LocalDateTime fecha, int pieza,
                                Superficie superficie, EstadoDental estado, String nota) {
        return hallazgos.save(Hallazgo.builder()
                .paciente(p)
                .consulta(consulta)
                .pieza(pieza)
                .superficie(superficie)
                .estado(estado)
                .nota(nota == null || nota.isBlank() ? null : nota.trim())
                .fecha(fecha)
                .registradoPor(consulta != null && consulta.getOdontologo() != null
                        ? consulta.getOdontologo().getNombre() : sesion.nombre())
                .build());
    }

    /** Valida que el hallazgo tenga sentido clínico antes de guardarlo. */
    public void validar(Paciente p, int pieza, Superficie superficie, EstadoDental estado) {
        if (!Pieza.esValida(pieza)) {
            throw new NegocioException("La pieza " + pieza + " no existe en la notación FDI.");
        }
        if (superficie == Superficie.PIEZA && !estado.seAplicaAPieza()) {
            throw new NegocioException(estado.getEtiqueta() + " se registra sobre una cara: "
                    + "haz clic en la cara afectada de la pieza " + pieza + ".");
        }
        if (superficie != Superficie.PIEZA && !estado.seAplicaACara()) {
            throw new NegocioException(estado.getEtiqueta() + " afecta a la pieza completa; "
                    + "se registrará en toda la pieza " + pieza + ".");
        }
        if (superficie != Superficie.PIEZA) {
            EstadoDental general = odontograma(p).pieza(pieza).general();
            if (general != null && general.anulaCaras()) {
                throw new NegocioException("La pieza " + pieza + " tiene registrado \""
                        + general.getEtiqueta().toLowerCase() + "\": no tiene caras naturales. "
                        + "Usa \"Sano\" sobre la pieza para restablecerla si fue un error.");
            }
        }
    }

    private static boolean sinCambio(Odontograma.EstadoPieza actual, Superficie s, EstadoDental e) {
        if (s == Superficie.PIEZA) {
            return e == EstadoDental.SANO ? actual.sana() : actual.general() == e;
        }
        EstadoDental cara = actual.cara(s);
        return e == EstadoDental.SANO ? cara == null : cara == e;
    }

    /**
     * Deshace el último hallazgo registrado hoy para el paciente.
     *
     * @return el hallazgo eliminado, o null si hoy no hay nada que deshacer
     */
    public Hallazgo deshacerUltimo(Paciente p) {
        sesion.exigirClinica();
        LocalDate hoy = LocalDate.now();
        return consultas.findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
                        p, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay().minusNanos(1))
                .flatMap(c -> hallazgos.findFirstByPacienteAndConsultaOrderByIdDesc(p, c).map(h -> {
                    hallazgos.delete(h);
                    // Una consulta abierta solo por ese registro desaparece con él.
                    if (hallazgos.findByConsultaOrderByIdAsc(c).isEmpty() && !items.existsByConsulta(c)) {
                        consultas.delete(c);
                    }
                    return h;
                }))
                .orElse(null);
    }
}
