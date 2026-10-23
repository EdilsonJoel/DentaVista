package com.erejm.dentavista.servicio;

import com.erejm.dentavista.dominio.Medicamento;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Receta;
import com.erejm.dentavista.repositorio.RecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Recetas: el odontólogo prescribe y el sistema cruza cada medicamento con las
 * alergias y antecedentes del paciente antes de emitirla.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RecetaService {

    private final RecetaRepository recetas;
    private final ClinicaService clinica;
    private final SesionService sesion;

    @Transactional(readOnly = true)
    public List<Receta> recetas(Paciente p) {
        sesion.exigirAccesoA(p);
        return recetas.findByPacienteOrderByFechaDesc(p);
    }

    /** Todas las alertas de la receta para este paciente. */
    public List<String> alertas(Paciente p, List<Receta.Item> items) {
        List<String> r = new ArrayList<>();
        for (Receta.Item i : items) {
            r.addAll(Medicamento.alertas(i.getMedicamento(), p.getAlergias(), p.getAntecedentes()));
        }
        return r;
    }

    /**
     * Emite la receta en la consulta de hoy.
     *
     * @param aceptarAlertas el odontólogo confirmó que vio las alertas (si las hay)
     */
    public Receta emitir(Paciente p, List<Receta.Item> items, String indicaciones, boolean aceptarAlertas) {
        sesion.exigirClinica();
        List<Receta.Item> limpios = items.stream()
                .filter(i -> i.getMedicamento() != null && !i.getMedicamento().isBlank())
                .map(i -> new Receta.Item(i.getMedicamento().trim(),
                        i.getIndicacion() == null || i.getIndicacion().isBlank() ? "Según indicación" : i.getIndicacion().trim()))
                .toList();
        if (limpios.isEmpty()) {
            throw new NegocioException("Agrega al menos un medicamento.");
        }
        List<String> alertas = alertas(p, limpios);
        if (!alertas.isEmpty() && !aceptarAlertas) {
            throw new NegocioException(String.join("\n", alertas));
        }
        return recetas.save(Receta.builder()
                .paciente(p)
                .odontologo(sesion.getUsuario())
                .consulta(clinica.consultaDeHoy(p))
                .fecha(LocalDateTime.now())
                .items(new ArrayList<>(limpios))
                .indicaciones(indicaciones == null || indicaciones.isBlank() ? null : indicaciones.trim())
                .alertasAceptadas(alertas.isEmpty() ? null : String.join(" | ", alertas))
                .build());
    }

    /** Uso interno (datos de demostración). */
    public Receta guardar(Receta r) {
        return recetas.save(r);
    }
}
