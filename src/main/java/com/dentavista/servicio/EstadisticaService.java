package com.dentavista.servicio;

import com.dentavista.modelo.Consulta;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Indicadores de la pantalla de inicio (los de dinero vienen de {@link PagoService}).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstadisticaService {

    public record Resumen(long pacientes, long consultasMes) {
    }

    private final PacienteRepository pacientes;
    private final ConsultaRepository consultas;

    public Resumen resumen() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finMes = hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay().minusNanos(1);
        return new Resumen(pacientes.countByActivoTrue(), consultas.countByFechaBetween(inicioMes, finMes));
    }

    public List<Consulta> ultimasConsultas() {
        return consultas.findTop8ByOrderByFechaDesc();
    }
}
