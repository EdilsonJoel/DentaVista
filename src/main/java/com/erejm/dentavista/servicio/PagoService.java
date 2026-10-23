package com.erejm.dentavista.servicio;

import com.erejm.dentavista.modelo.MedioPago;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Pago;
import com.erejm.dentavista.repositorio.ItemPlanRepository;
import com.erejm.dentavista.repositorio.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caja del consultorio: abonos de los pacientes contra su plan de tratamiento.
 * <p>
 * La <b>deuda</b> es lo realizado que todavía no se pagó; un paciente también puede
 * pagar por adelantado los tratamientos pendientes (queda <b>a favor</b>), pero nunca
 * más que el total de su plan.
 * </p>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PagoService {

    /** Estado de cuenta de un paciente. */
    public record Cuenta(BigDecimal totalPlan, BigDecimal realizado, BigDecimal pagado) {

        /** Lo realizado que falta pagar. */
        public BigDecimal deuda() {
            return realizado.subtract(pagado).max(BigDecimal.ZERO);
        }

        /** Pagado por adelantado (aún no se hizo el tratamiento). */
        public BigDecimal aFavor() {
            return pagado.subtract(realizado).max(BigDecimal.ZERO);
        }

        /** Lo máximo que se puede cobrar hoy: el resto del plan completo. */
        public BigDecimal porPagarDelPlan() {
            return totalPlan.subtract(pagado).max(BigDecimal.ZERO);
        }

        /** Avance del pago respecto al plan (0..1). */
        public double avance() {
            return totalPlan.signum() == 0 ? 0 : Math.min(1, pagado.doubleValue() / totalPlan.doubleValue());
        }
    }

    private final PagoRepository pagos;
    private final ItemPlanRepository items;
    private final PlanService planes;
    private final com.erejm.dentavista.repositorio.PacienteRepository pacientesRepo;
    private final SesionService sesion;

    @Transactional(readOnly = true)
    public Cuenta cuenta(Paciente p) {
        sesion.exigirAccesoA(p);
        PlanService.Totales t = planes.totales(p);
        return new Cuenta(t.total(), t.realizado(), pagos.pagadoPor(p));
    }

    @Transactional(readOnly = true)
    public List<Pago> pagos(Paciente p) {
        sesion.exigirAccesoA(p);
        return pagos.findByPacienteOrderByFechaDesc(p);
    }

    public Pago registrar(Paciente p, BigDecimal monto, MedioPago medio, String nota) {
        sesion.exigirPersonal();
        if (monto == null || monto.signum() <= 0) {
            throw new NegocioException("El monto debe ser mayor a cero.");
        }
        if (medio == null) {
            throw new NegocioException("Elige el medio de pago.");
        }
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        Cuenta c = cuenta(p);
        if (m.compareTo(c.porPagarDelPlan()) > 0) {
            throw new NegocioException(c.porPagarDelPlan().signum() == 0
                    ? "El plan de " + p.nombreCompleto() + " ya está pagado por completo."
                    : "El pago supera lo que falta del plan. Máximo: S/ " + c.porPagarDelPlan().setScale(2, RoundingMode.HALF_UP) + ".");
        }
        return guardar(Pago.builder()
                .paciente(p)
                .fecha(LocalDateTime.now())
                .monto(m)
                .medio(medio)
                .nota(nota == null || nota.isBlank() ? null : nota.trim())
                .registradoPor(sesion.nombre())
                .build());
    }

    /** Uso interno (también datos de demostración): numera el recibo y guarda. */
    public Pago guardar(Pago pago) {
        int anio = pago.getFecha().getYear();
        String prefijo = "R-" + anio + "-";
        pago.setRecibo(prefijo + String.format("%04d", pagos.countByReciboStartingWith(prefijo) + 1));
        return pagos.save(pago);
    }

    public Pago anular(Pago pago, String motivo) {
        sesion.exigirPersonal();
        if (motivo == null || motivo.isBlank()) {
            throw new NegocioException("Indica el motivo de la anulación.");
        }
        Pago actual = pagos.findById(pago.getId()).orElseThrow(() -> new NegocioException("El pago ya no existe."));
        if (actual.isAnulado()) {
            throw new NegocioException("El recibo " + actual.getRecibo() + " ya está anulado.");
        }
        actual.setAnulado(true);
        actual.setMotivoAnulacion(motivo.trim());
        return pagos.save(actual);
    }

    // ---------------------------------------------------------- indicadores

    @Transactional(readOnly = true)
    public BigDecimal cobradoEsteMes() {
        LocalDate hoy = LocalDate.now();
        return pagos.cobradoEntre(hoy.withDayOfMonth(1).atStartOfDay(), hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public long pagosEsteMes() {
        LocalDate hoy = LocalDate.now();
        return pagos.pagosEntre(hoy.withDayOfMonth(1).atStartOfDay(), hoy.plusMonths(1).withDayOfMonth(1).atStartOfDay());
    }

    /** Paciente con saldo pendiente. */
    public record Deudor(Paciente paciente, BigDecimal deuda) {
    }

    /** Pacientes que deben tratamientos ya realizados, de mayor a menor deuda. */
    @Transactional(readOnly = true)
    public List<Deudor> deudores() {
        sesion.exigirPersonal();
        List<Deudor> r = new java.util.ArrayList<>();
        saldos().forEach((id, deuda) -> {
            if (deuda.signum() > 0) {
                pacientesRepo.findById(id).filter(Paciente::isActivo).ifPresent(p -> r.add(new Deudor(p, deuda)));
            }
        });
        r.sort(java.util.Comparator.comparing(Deudor::deuda).reversed());
        return r;
    }

    /** Total que deben los pacientes activos por tratamientos ya realizados. */
    public BigDecimal porCobrar() {
        return deudores().stream().map(Deudor::deuda).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Saldo de cada paciente con tratamientos realizados: realizado menos pagado
     * (positivo = debe; cero o negativo = al día).
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> saldos() {
        Map<Long, BigDecimal> pagado = new HashMap<>();
        for (Object[] f : pagos.pagadoPorPaciente()) {
            pagado.put(((Number) f[0]).longValue(), (BigDecimal) f[1]);
        }
        Map<Long, BigDecimal> r = new HashMap<>();
        for (Object[] f : items.realizadoPorPaciente()) {
            long id = ((Number) f[0]).longValue();
            r.put(id, ((BigDecimal) f[1]).subtract(pagado.getOrDefault(id, BigDecimal.ZERO)));
        }
        return r;
    }
}
