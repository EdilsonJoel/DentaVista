package com.erejm.dentavista.datos;

import com.erejm.dentavista.modelo.EstadoItem;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.repositorio.ConsultaRepository;
import com.erejm.dentavista.repositorio.PacienteRepository;
import com.erejm.dentavista.repositorio.UsuarioRepository;
import com.erejm.dentavista.servicio.ClinicaService;
import com.erejm.dentavista.servicio.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los datos de demostración se cargan completos y son clínicamente coherentes.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@SpringBootTest
class DatosDemoTest {

    @Autowired private PacienteRepository pacientes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ConsultaRepository consultas;
    @Autowired private ClinicaService clinica;
    @Autowired private PlanService plan;
    @Autowired private DatosDemo demo;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        Path carpeta = Files.createTempDirectory("dentavista-demo");
        carpeta.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + carpeta.resolve("demo.db"));
        registro.add("app.demo.enabled", () -> "true");
    }

    @Test
    void cargaUsuariosPacientesYConsultas() {
        assertThat(usuarios.count()).isEqualTo(5);
        assertThat(pacientes.count()).isEqualTo(12);
        assertThat(consultas.count()).isGreaterThan(25);
    }

    @Test
    void esIdempotente() {
        demo.run();
        assertThat(pacientes.count()).isEqualTo(12);
    }

    @Test
    void cadaPacienteTieneHistoriaYPlanCoherente() {
        for (Paciente p : pacientes.findAll()) {
            assertThat(clinica.consultas(p)).as("consultas de %s", p.nombreCompleto()).isNotEmpty();
            // Toda patología abierta tiene un tratamiento pendiente en el plan.
            long pendientes = plan.items(p).stream().filter(i -> i.getEstado() == EstadoItem.PENDIENTE).count();
            assertThat(pendientes).as("plan de %s", p.nombreCompleto())
                    .isGreaterThanOrEqualTo(clinica.odontograma(p).patologias().size());
        }
    }

    @Test
    void laPacienteDemoMuestraEvolucionEnElTiempo() {
        Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        var primera = clinica.consultasCronologicas(valeria).get(0);
        var alInicio = clinica.odontograma(valeria, primera.getFecha().plusHours(1));
        var hoy = clinica.odontograma(valeria);
        assertThat(alInicio.pieza(16).cara(com.erejm.dentavista.dominio.Superficie.OCLUSAL))
                .isEqualTo(com.erejm.dentavista.dominio.EstadoDental.CARIES);
        assertThat(hoy.pieza(16).cara(com.erejm.dentavista.dominio.Superficie.OCLUSAL))
                .isEqualTo(com.erejm.dentavista.dominio.EstadoDental.OBTURADO);
        assertThat(hoy.pieza(46).general()).isEqualTo(com.erejm.dentavista.dominio.EstadoDental.CORONA);
    }
}
