package com.erejm.dentavista.servicio;

import com.erejm.dentavista.IntegracionBase;
import com.erejm.dentavista.modelo.EstadoItem;
import com.erejm.dentavista.modelo.ItemPlan;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static com.erejm.dentavista.dominio.EstadoDental.*;
import static com.erejm.dentavista.dominio.Superficie.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ciclo completo: diagnóstico → plan → realizado → odontograma actualizado.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class PlanServiceTest extends IntegracionBase {

    @Autowired
    private ClinicaService clinica;
    @Autowired
    private PlanService plan;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        iniciarSesion(Rol.ODONTOLOGO);
        paciente = pacienteNuevo();
        tratamiento("OBT", "90", OBTURADO, true);
        tratamiento("EXO", "150", AUSENTE, false);
        tratamiento("END", "480", ENDODONCIA, false);
    }

    @Test
    void generaUnTratamientoPorCadaPatologiaSinDuplicar() {
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.registrar(paciente, 16, MESIAL, CARIES, null);
        clinica.registrar(paciente, 48, PIEZA, EXTRACCION_INDICADA, null);

        assertThat(plan.generarDesdeOdontograma(paciente)).isEqualTo(3);
        assertThat(plan.generarDesdeOdontograma(paciente)).isZero();

        List<ItemPlan> items = plan.items(paciente);
        assertThat(items).extracting(i -> i.getTratamiento().getCodigo())
                .containsExactlyInAnyOrder("OBT", "OBT", "EXO");
        assertThat(plan.totales(paciente).pendiente()).isEqualByComparingTo(new BigDecimal("330"));
    }

    @Test
    void realizarUnaObturacionCuraLaCariesEnElOdontograma() {
        clinica.registrar(paciente, 26, OCLUSAL, CARIES, null);
        plan.generarDesdeOdontograma(paciente);
        ItemPlan obt = plan.items(paciente).get(0);

        plan.realizar(obt);

        assertThat(clinica.odontograma(paciente).pieza(26).cara(OCLUSAL)).isEqualTo(OBTURADO);
        assertThat(clinica.odontograma(paciente).patologias()).isEmpty();
        PlanService.Totales t = plan.totales(paciente);
        assertThat(t.realizado()).isEqualByComparingTo("90");
        assertThat(t.pendiente()).isEqualByComparingTo("0");
        assertThat(t.avance()).isEqualTo(1.0);
    }

    @Test
    void realizarUnaExtraccionDejaLaPiezaAusente() {
        clinica.registrar(paciente, 38, PIEZA, EXTRACCION_INDICADA, null);
        plan.generarDesdeOdontograma(paciente);
        plan.realizar(plan.items(paciente).get(0));
        assertThat(clinica.odontograma(paciente).pieza(38).ausente()).isTrue();
    }

    @Test
    void elPrecioQuedaCongeladoAunqueCambieElCatalogo() {
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        plan.generarDesdeOdontograma(paciente);
        var obt = tratamientos.findByCodigo("OBT").orElseThrow();
        obt.setPrecio(new BigDecimal("200"));
        tratamientos.save(obt);
        assertThat(plan.items(paciente).get(0).getPrecio()).isEqualByComparingTo("90");
    }

    @Test
    void unaObturacionManualExigeElegirLaCara() {
        var obt = tratamientos.findByCodigo("OBT").orElseThrow();
        assertThatThrownBy(() -> plan.agregar(paciente, obt, 16, null))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("cara");
        assertThat(plan.agregar(paciente, obt, 16, DISTAL).getSuperficie()).isEqualTo(DISTAL);
    }

    @Test
    void cancelarYReabrir() {
        var end = tratamientos.findByCodigo("END").orElseThrow();
        ItemPlan i = plan.agregar(paciente, end, 46, null);
        assertThat(plan.cancelar(i).getEstado()).isEqualTo(EstadoItem.CANCELADO);
        assertThatThrownBy(() -> plan.realizar(i)).isInstanceOf(NegocioException.class);
        assertThat(plan.reabrir(i).getEstado()).isEqualTo(EstadoItem.PENDIENTE);
    }
}
