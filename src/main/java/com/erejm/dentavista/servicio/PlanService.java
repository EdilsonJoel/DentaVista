package com.erejm.dentavista.servicio;

import com.erejm.dentavista.dominio.EstadoDental;
import com.erejm.dentavista.dominio.Odontograma;
import com.erejm.dentavista.dominio.Pieza;
import com.erejm.dentavista.dominio.Superficie;
import com.erejm.dentavista.modelo.Consulta;
import com.erejm.dentavista.modelo.EstadoItem;
import com.erejm.dentavista.modelo.ItemPlan;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Tratamiento;
import com.erejm.dentavista.repositorio.ItemPlanRepository;
import com.erejm.dentavista.repositorio.TratamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Plan de tratamiento y presupuesto. Cierra el ciclo clínico:
 * <b>diagnóstico</b> (hallazgos) → <b>plan</b> (generado automáticamente) →
 * <b>presupuesto</b> (PDF) → <b>tratamiento realizado</b>, que a su vez actualiza
 * el odontograma (la caries pasa a obturación, la extracción deja la pieza ausente...).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {

    /** Totales del plan de un paciente. */
    public record Totales(BigDecimal pendiente, BigDecimal realizado, int itemsPendientes, int itemsRealizados) {
        public BigDecimal total() {
            return pendiente.add(realizado);
        }

        /** Avance del plan entre 0 y 1 (por importe). */
        public double avance() {
            BigDecimal t = total();
            return t.signum() == 0 ? 0 : realizado.doubleValue() / t.doubleValue();
        }
    }

    private final ItemPlanRepository items;
    private final TratamientoRepository tratamientos;
    private final ClinicaService clinica;
    private final SesionService sesion;

    @Transactional(readOnly = true)
    public List<ItemPlan> items(Paciente p) {
        sesion.exigirAccesoA(p);
        return items.findByPacienteOrderByCreadoAscIdAsc(p);
    }

    @Transactional(readOnly = true)
    public Totales totales(Paciente p) {
        BigDecimal pendiente = BigDecimal.ZERO;
        BigDecimal realizado = BigDecimal.ZERO;
        int np = 0;
        int nr = 0;
        for (ItemPlan i : items(p)) {
            if (i.getEstado() == EstadoItem.PENDIENTE) {
                pendiente = pendiente.add(i.getPrecio());
                np++;
            } else if (i.getEstado() == EstadoItem.REALIZADO) {
                realizado = realizado.add(i.getPrecio());
                nr++;
            }
        }
        return new Totales(pendiente, realizado, np, nr);
    }

    /**
     * Agrega al plan un tratamiento por cada patología del odontograma que todavía
     * no tenga uno pendiente (caries → obturación, extracción indicada → exodoncia...).
     *
     * @return cuántos tratamientos se agregaron
     */
    public int generarDesdeOdontograma(Paciente p) {
        sesion.exigirClinica();
        Odontograma o = clinica.odontograma(p);
        List<ItemPlan> pendientes = items.findByPacienteAndEstado(p, EstadoItem.PENDIENTE);
        int agregados = 0;
        for (Odontograma.Patologia pat : o.patologias()) {
            Tratamiento t = tratamientos.findByCodigo(pat.estado().getTratamientoSugerido())
                    .filter(Tratamiento::isActivo)
                    .orElse(null);
            if (t == null) {
                continue;
            }
            Superficie s = t.isPorCara() ? pat.superficie() : Superficie.PIEZA;
            boolean yaEsta = pendientes.stream().anyMatch(i ->
                    i.getTratamiento().getId().equals(t.getId())
                            && Objects.equals(i.getPieza(), pat.pieza())
                            && i.getSuperficie() == s);
            if (!yaEsta) {
                pendientes.add(items.save(nuevo(p, t, pat.pieza(), s, LocalDateTime.now(),
                        pat.estado().getEtiqueta() + " en " + pat.superficie().etiqueta(pat.pieza()).toLowerCase())));
                agregados++;
            }
        }
        return agregados;
    }

    /** Agrega un tratamiento manualmente. */
    public ItemPlan agregar(Paciente p, Tratamiento t, Integer pieza, Superficie superficie) {
        sesion.exigirClinica();
        if (t == null) {
            throw new NegocioException("Elige un tratamiento del catálogo.");
        }
        if (t.isRequierePieza() && pieza == null) {
            throw new NegocioException(t.getNombre() + " se realiza sobre una pieza: elige cuál.");
        }
        if (pieza != null && !Pieza.esValida(pieza)) {
            throw new NegocioException("La pieza " + pieza + " no existe en la notación FDI.");
        }
        Superficie s = superficie;
        if (!t.isRequierePieza()) {
            pieza = null;
            s = null;
        } else if (t.isPorCara()) {
            if (s == null || !s.esCara()) {
                throw new NegocioException(t.getNombre() + " se aplica por cara: elige la cara de la pieza " + pieza + ".");
            }
        } else {
            s = Superficie.PIEZA;
        }
        return items.save(nuevo(p, t, pieza, s, LocalDateTime.now(), null));
    }

    /**
     * Marca el tratamiento como realizado en la consulta de hoy y registra en el
     * odontograma el estado resultante.
     */
    public ItemPlan realizar(ItemPlan item) {
        sesion.exigirClinica();
        ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != EstadoItem.PENDIENTE) {
            throw new NegocioException("Solo se pueden realizar tratamientos pendientes.");
        }
        Paciente p = i.getPaciente();
        EstadoDental resultado = i.getTratamiento().getEstadoResultante();
        if (resultado != null && i.getPieza() != null) {
            Superficie s = i.getSuperficie() != null && i.getSuperficie().esCara() && resultado.seAplicaACara()
                    ? i.getSuperficie() : Superficie.PIEZA;
            clinica.validar(p, i.getPieza(), s, resultado);
        }
        Consulta hoy = clinica.consultaDeHoy(p);
        LocalDateTime ahora = LocalDateTime.now();
        aplicarRealizacion(i, hoy, ahora);
        return items.save(i);
    }

    /** Usado también por los datos de demostración para fechar tratamientos en el pasado. */
    public void aplicarRealizacion(ItemPlan i, Consulta consulta, LocalDateTime fecha) {
        i.setEstado(EstadoItem.REALIZADO);
        i.setRealizado(fecha);
        i.setConsulta(consulta);
        EstadoDental resultado = i.getTratamiento().getEstadoResultante();
        if (resultado != null && i.getPieza() != null) {
            Superficie s = i.getSuperficie() != null && i.getSuperficie().esCara() && resultado.seAplicaACara()
                    ? i.getSuperficie() : Superficie.PIEZA;
            clinica.registrarEn(i.getPaciente(), consulta, fecha, i.getPieza(), s, resultado,
                    i.getTratamiento().getNombre());
        }
        items.save(i);
    }

    public ItemPlan cancelar(ItemPlan item) {
        sesion.exigirClinica();
        ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != EstadoItem.PENDIENTE) {
            throw new NegocioException("Solo se pueden cancelar tratamientos pendientes.");
        }
        i.setEstado(EstadoItem.CANCELADO);
        return items.save(i);
    }

    public ItemPlan reabrir(ItemPlan item) {
        sesion.exigirClinica();
        ItemPlan i = items.findById(item.getId())
                .orElseThrow(() -> new NegocioException("El tratamiento ya no existe."));
        if (i.getEstado() != EstadoItem.CANCELADO) {
            throw new NegocioException("Solo se pueden reabrir tratamientos cancelados.");
        }
        i.setEstado(EstadoItem.PENDIENTE);
        return items.save(i);
    }

    public ItemPlan nuevo(Paciente p, Tratamiento t, Integer pieza, Superficie s, LocalDateTime creado, String nota) {
        return ItemPlan.builder()
                .paciente(p)
                .tratamiento(t)
                .pieza(pieza)
                .superficie(s)
                .precio(t.getPrecio())
                .estado(EstadoItem.PENDIENTE)
                .creado(creado)
                .nota(nota)
                .build();
    }

    public ItemPlan guardar(ItemPlan i) {
        return items.save(i);
    }
}
