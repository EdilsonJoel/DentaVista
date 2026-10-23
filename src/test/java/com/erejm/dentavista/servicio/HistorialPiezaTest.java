package com.erejm.dentavista.servicio;

import com.erejm.dentavista.IntegracionBase;
import com.erejm.dentavista.modelo.EstadoItem;
import com.erejm.dentavista.modelo.ItemPlan;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.erejm.dentavista.dominio.EstadoDental.CARIES;
import static com.erejm.dentavista.dominio.EstadoDental.OBTURADO;
import static com.erejm.dentavista.dominio.EstadoDental.SANO;
import static com.erejm.dentavista.dominio.Superficie.DISTAL;
import static com.erejm.dentavista.dominio.Superficie.OCLUSAL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Historial de una sola pieza: solo lo de esa pieza, en orden, con su estado tras cada
 * consulta y lo invertido en ella.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class HistorialPiezaTest extends IntegracionBase {

    @Autowired private HistorialPiezaService historiales;
    @Autowired private ClinicaService clinica;
    @Autowired private PlanService planes;

    @Test
    void reuneSoloLaPiezaPedidaConSuEvolucionEImportes() {
        iniciarSesion(Rol.ODONTOLOGO);
        tratamiento("OBT", "90", OBTURADO, true);
        Paciente p = pacienteNuevo();
        clinica.registrar(p, 16, OCLUSAL, CARIES, "Lesión profunda");
        clinica.registrar(p, 16, DISTAL, CARIES, null);
        clinica.registrar(p, 26, OCLUSAL, CARIES, null);
        planes.generarDesdeOdontograma(p);
        ItemPlan oclusal16 = planes.items(p).stream()
                .filter(i -> i.getPieza() == 16 && i.getSuperficie() == OCLUSAL).findFirst().orElseThrow();
        planes.realizar(oclusal16);

        HistorialPiezaService.Historial h = historiales.de(p, 16);
        assertThat(h.nombre()).isEqualTo("Primer molar superior derecho");
        assertThat(h.eventos()).isNotEmpty();
        assertThat(h.eventos()).allSatisfy(e -> assertThat(e.hallazgos()).allMatch(x -> x.getPieza() == 16));
        assertThat(h.eventos().stream().flatMap(e -> e.hallazgos().stream()).count())
                .as("2 caries + la obturación al realizar").isGreaterThanOrEqualTo(3);
        assertThat(h.tratamientosRealizados()).isEqualTo(1);
        assertThat(h.realizado()).isEqualByComparingTo("90");
        assertThat(h.pendientes()).hasSize(1).allMatch(i -> i.getEstado() == EstadoItem.PENDIENTE);
        assertThat(h.porRealizar()).isEqualByComparingTo("90");
        // Hoy: oclusal obturada, distal con caries.
        assertThat(h.actual().visible(OCLUSAL)).isEqualTo(OBTURADO);
        assertThat(h.actual().visible(DISTAL)).isEqualTo(CARIES);
        assertThat(h.eventos().get(h.eventos().size() - 1).estadoDespues().visible(OCLUSAL)).isEqualTo(OBTURADO);

        // Una pieza sin registros tiene un historial vacío y sana.
        HistorialPiezaService.Historial sana = historiales.de(p, 11);
        assertThat(sana.eventos()).isEmpty();
        assertThat(sana.actual().visible(OCLUSAL)).isEqualTo(SANO);
    }
}
