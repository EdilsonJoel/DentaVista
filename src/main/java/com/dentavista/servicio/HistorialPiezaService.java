package com.dentavista.servicio;

import com.dentavista.dominio.Odontograma;
import com.dentavista.dominio.Pieza;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.Hallazgo;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Historia clínica de UNA sola pieza dental: qué se encontró en cada cara, qué se le
 * hizo, quién lo hizo y cómo quedó después de cada consulta. Se arma a partir de los
 * hallazgos registrados (el odontograma se reconstruye a cada fecha) y del plan de
 * tratamiento.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistorialPiezaService {

    /** Lo que pasó con la pieza en una consulta y cómo quedó al terminarla. */
    public record Evento(LocalDateTime fecha, Consulta consulta, List<Hallazgo> hallazgos, List<ItemPlan> realizados,
                         Odontograma.EstadoPieza estadoDespues) {
    }

    public record Historial(Paciente paciente, int fdi, String nombre, Odontograma.EstadoPieza actual,
                            List<Evento> eventos, List<ItemPlan> pendientes, BigDecimal realizado,
                            BigDecimal porRealizar) {

        /** Primera consulta en la que se registró algo en la pieza (o null). */
        public LocalDateTime primeraAtencion() {
            return eventos.isEmpty() ? null : eventos.get(0).fecha();
        }

        public long tratamientosRealizados() {
            return eventos.stream().mapToLong(e -> e.realizados().size()).sum();
        }
    }

    private final ClinicaService clinica;
    private final PlanService planes;

    public Historial de(Paciente p, int fdi) {
        Pieza.validar(fdi);
        List<Hallazgo> hallazgos = new ArrayList<>(clinica.historialPieza(p, fdi));
        hallazgos.sort(Comparator.comparing(Hallazgo::getFecha).thenComparing(Hallazgo::getId));
        List<ItemPlan> items = planes.items(p).stream()
                .filter(i -> Objects.equals(i.getPieza(), fdi))
                .toList();

        // Agrupa por consulta, en orden cronológico.
        Map<Long, List<Hallazgo>> porConsulta = new LinkedHashMap<>();
        Map<Long, Consulta> consultas = new LinkedHashMap<>();
        for (Hallazgo h : hallazgos) {
            Long clave = h.getConsulta() == null ? -h.getId() : h.getConsulta().getId();
            porConsulta.computeIfAbsent(clave, k -> new ArrayList<>()).add(h);
            consultas.putIfAbsent(clave, h.getConsulta());
        }
        for (ItemPlan i : items) {
            if (i.getEstado() == EstadoItem.REALIZADO && i.getConsulta() != null) {
                porConsulta.putIfAbsent(i.getConsulta().getId(), new ArrayList<>());
                consultas.putIfAbsent(i.getConsulta().getId(), i.getConsulta());
            }
        }

        List<Evento> eventos = new ArrayList<>();
        for (Map.Entry<Long, List<Hallazgo>> e : porConsulta.entrySet()) {
            Consulta c = consultas.get(e.getKey());
            List<ItemPlan> realizados = items.stream()
                    .filter(i -> i.getEstado() == EstadoItem.REALIZADO && i.getConsulta() != null && c != null
                            && i.getConsulta().getId().equals(c.getId()))
                    .toList();
            LocalDateTime fecha = c != null ? c.getFecha() : e.getValue().get(0).getFecha();
            // Estado al cerrar la consulta: el último registro de ese día en la pieza.
            LocalDateTime corte = e.getValue().stream().map(Hallazgo::getFecha).max(Comparator.naturalOrder())
                    .orElse(fecha);
            for (ItemPlan i : realizados) {
                if (i.getRealizado() != null && i.getRealizado().isAfter(corte)) {
                    corte = i.getRealizado();
                }
            }
            Odontograma.EstadoPieza despues = clinica.odontograma(p, corte).pieza(fdi);
            eventos.add(new Evento(fecha, c, e.getValue(), realizados, despues));
        }
        eventos.sort(Comparator.comparing(Evento::fecha));

        List<ItemPlan> pendientes = items.stream().filter(i -> i.getEstado() == EstadoItem.PENDIENTE).toList();
        BigDecimal realizado = items.stream().filter(i -> i.getEstado() == EstadoItem.REALIZADO)
                .map(ItemPlan::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal porRealizar = pendientes.stream().map(ItemPlan::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Historial(p, fdi, Pieza.nombre(fdi), clinica.odontograma(p).pieza(fdi), eventos, pendientes,
                realizado, porRealizar);
    }
}
