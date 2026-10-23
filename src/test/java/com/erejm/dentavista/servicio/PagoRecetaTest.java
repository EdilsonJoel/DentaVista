package com.erejm.dentavista.servicio;

import com.erejm.dentavista.IntegracionBase;
import com.erejm.dentavista.dominio.Medicamento;
import com.erejm.dentavista.modelo.MedioPago;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Pago;
import com.erejm.dentavista.modelo.Receta;
import com.erejm.dentavista.modelo.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static com.erejm.dentavista.dominio.EstadoDental.CARIES;
import static com.erejm.dentavista.dominio.EstadoDental.OBTURADO;
import static com.erejm.dentavista.dominio.Superficie.OCLUSAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Caja (pagos y saldo) y recetas con alertas de alergia.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class PagoRecetaTest extends IntegracionBase {

    @Autowired private PagoService pagos;
    @Autowired private RecetaService recetas;
    @Autowired private ClinicaService clinica;
    @Autowired private PlanService planes;

    private Paciente paciente;

    @BeforeEach
    void preparar() {
        iniciarSesion(Rol.ODONTOLOGO);
        tratamiento("OBT", "90", OBTURADO, true);
        paciente = pacienteNuevo();
        // Plan: dos obturaciones (S/ 180); una realizada (S/ 90).
        clinica.registrar(paciente, 16, OCLUSAL, CARIES, null);
        clinica.registrar(paciente, 26, OCLUSAL, CARIES, null);
        planes.generarDesdeOdontograma(paciente);
        planes.realizar(planes.items(paciente).get(0));
    }

    @Test
    void laDeudaEsLoRealizadoMenosLoPagado() {
        PagoService.Cuenta c = pagos.cuenta(paciente);
        assertThat(c.totalPlan()).isEqualByComparingTo("180");
        assertThat(c.deuda()).isEqualByComparingTo("90");

        Pago p = pagos.registrar(paciente, new BigDecimal("50"), MedioPago.YAPE, "Operación 123");
        assertThat(p.getRecibo()).matches("R-\\d{4}-\\d{4}");
        assertThat(pagos.cuenta(paciente).deuda()).isEqualByComparingTo("40");
    }

    @Test
    void pagarElSaldoDejaNadaPorPagar() {
        pagos.registrar(paciente, pagos.cuenta(paciente).porPagarDelPlan(), MedioPago.EFECTIVO, null);
        PagoService.Cuenta c = pagos.cuenta(paciente);
        assertThat(c.porPagarDelPlan()).isEqualByComparingTo("0");
        assertThat(c.deuda()).isEqualByComparingTo("0");
        assertThat(c.pagado()).isEqualByComparingTo("180");
    }

    @Test
    void sePuedePagarPorAdelantadoPeroNoMasQueElPlan() {
        pagos.registrar(paciente, new BigDecimal("150"), MedioPago.EFECTIVO, null);
        PagoService.Cuenta c = pagos.cuenta(paciente);
        assertThat(c.deuda()).isEqualByComparingTo("0");
        assertThat(c.aFavor()).isEqualByComparingTo("60");
        assertThatThrownBy(() -> pagos.registrar(paciente, new BigDecimal("31"), MedioPago.EFECTIVO, null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("Máximo: S/ 30.00");
        assertThatThrownBy(() -> pagos.registrar(paciente, BigDecimal.ZERO, MedioPago.EFECTIVO, null))
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void unReciboAnuladoDejaDeContar() {
        Pago p = pagos.registrar(paciente, new BigDecimal("90"), MedioPago.TARJETA, null);
        pagos.anular(p, "Monto mal digitado");
        assertThat(pagos.cuenta(paciente).deuda()).isEqualByComparingTo("90");
        assertThatThrownBy(() -> pagos.anular(p, "otra vez")).hasMessageContaining("ya está anulado");
    }

    @Test
    void laRecetaAvisaDeLaAlergiaALaPenicilina() {
        paciente.setAlergias("Penicilina");
        pacientes.save(paciente);
        List<Receta.Item> items = List.of(new Receta.Item("Amoxicilina 500 mg", "1 cada 8 horas"));
        assertThatThrownBy(() -> recetas.emitir(paciente, items, null, false))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ALERGIA");
        Receta r = recetas.emitir(paciente, items, null, true);
        assertThat(r.getAlertasAceptadas()).contains("penicilinas");
        assertThat(recetas.emitir(paciente, List.of(new Receta.Item("Clindamicina 300 mg", "")), null, false)
                .getItems().get(0).getIndicacion()).isEqualTo("Según indicación");
    }

    @Test
    void laRecepcionNoEmiteRecetas() {
        iniciarSesion(Rol.ASISTENTE);
        assertThatThrownBy(() -> recetas.emitir(paciente, List.of(new Receta.Item("Paracetamol 500 mg", "x")), null, false))
                .hasMessageContaining("no permite");
    }

    @Test
    void alertasDeContraindicaciones() {
        assertThat(Medicamento.alertas("Ibuprofeno 400 mg", null, "Anticoagulado (warfarina)"))
                .anyMatch(a -> a.contains("sangrado"));
        assertThat(Medicamento.alertas("Naproxeno 550 mg", "Ibuprofeno", null))
                .anyMatch(a -> a.contains("ALERGIA"));
        assertThat(Medicamento.alertas("Ketorolaco 10 mg", null, "Embarazo 2.º trimestre"))
                .anyMatch(a -> a.contains("embarazo"));
        assertThat(Medicamento.alertas("Paracetamol 500 mg", "Penicilina", "Embarazo")).isEmpty();
    }
}
