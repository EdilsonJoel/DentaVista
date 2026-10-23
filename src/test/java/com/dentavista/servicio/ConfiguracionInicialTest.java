package com.dentavista.servicio;

import com.dentavista.modelo.Consultorio;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.PacienteRepository;
import com.dentavista.repositorio.TratamientoRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Primer uso del producto: base vacía, sin cuentas de ejemplo. El asistente registra el
 * consultorio y el administrador; después ya no se puede volver a ejecutar.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfiguracionInicialTest {

    @Autowired private ConfiguracionInicialService configuracion;
    @Autowired private UsuarioService usuarios;
    @Autowired private DatosClinica clinica;
    @Autowired private TratamientoRepository tratamientos;
    @Autowired private PacienteRepository pacientes;
    @Autowired private SesionService sesion;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        Path carpeta = Files.createTempDirectory("dentavista-primer-uso");
        carpeta.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + carpeta.resolve("vacia.db"));
        registro.add("app.demo.enabled", () -> "false");
        registro.add("logging.level.com.dentavista", () -> "WARN");
    }

    private static Consultorio consultorio() {
        return Consultorio.builder().nombre("Clínica Dental Los Andes").ruc("20123456789")
                .direccion("Av. Circunvalación 120").telefono("051 33 2211").email("Citas@LosAndes.pe").build();
    }

    @Test
    @Order(1)
    void unaBaseNuevaNoTraeCuentasNiPacientesDeEjemplo() {
        assertThat(configuracion.requiereConfiguracion()).isTrue();
        assertThat(clinica.configurado()).isFalse();
        assertThat(pacientes.count()).isZero();
        assertThatThrownBy(() -> usuarios.autenticar("admin", "admin123")).hasMessage("Usuario o contraseña incorrectos.");
    }

    @Test
    @Order(2)
    void rechazaContrasenasDebilesSinDejarNadaAMedias() {
        var debil = new ConfiguracionInicialService.Administrador("Ana Ríos", "ana.rios", null, "12345678", "12345678");
        assertThatThrownBy(() -> configuracion.configurar(consultorio(), debil, false))
                .hasMessageContaining("letras y números");
        var distinta = new ConfiguracionInicialService.Administrador("Ana Ríos", "ana.rios", null, "clave1234", "clave9999");
        assertThatThrownBy(() -> configuracion.configurar(consultorio(), distinta, false))
                .hasMessageContaining("no coinciden");
        assertThat(clinica.configurado()).isFalse();
        assertThat(configuracion.requiereConfiguracion()).isTrue();
    }

    @Test
    @Order(3)
    void configuraConsultorioAdministradorYCatalogo() {
        var admin = new ConfiguracionInicialService.Administrador("Dra. Ana Ríos Mamani", "ana.rios",
                "ana@losandes.pe", "Sonrisa2026", "Sonrisa2026");
        Usuario cuenta = configuracion.configurar(consultorio(), admin, false);

        assertThat(cuenta.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(configuracion.requiereConfiguracion()).isFalse();
        assertThat(clinica.nombre()).isEqualTo("Clínica Dental Los Andes");
        assertThat(clinica.email()).isEqualTo("citas@losandes.pe");
        assertThat(clinica.lineaLegal()).isEqualTo("RUC 20123456789 · Av. Circunvalación 120");
        assertThat(tratamientos.count()).as("catálogo base de precios").isGreaterThan(5);
        assertThat(pacientes.count()).as("sin ejemplos").isZero();
        assertThat(usuarios.autenticar("ana.rios", "Sonrisa2026").requiereCambioClave()).isFalse();
    }

    @Test
    @Order(4)
    void noSePuedeVolverAConfigurar() {
        var otro = new ConfiguracionInicialService.Administrador("Intruso", "intruso", null, "clave1234", "clave1234");
        assertThatThrownBy(() -> configuracion.configurar(consultorio(), otro, true)).hasMessageContaining("ya está configurado");
    }

    @Test
    @Order(5)
    void elAdministradorCreaCuentasConContrasenaTemporal() {
        usuarios.autenticar("ana.rios", "Sonrisa2026");
        assertThat(usuarios.sugerirUsuario("Dr. Rubén Mamani Condori")).isEqualTo("ruben.mamani");
        assertThat(usuarios.sugerirUsuario("María de la Cruz")).isEqualTo("maria.cruz");

        Usuario odontologo = Usuario.builder().nombre("Dr. Rubén Mamani Condori").usuario("ruben.mamani")
                .rol(Rol.ODONTOLOGO).build();
        assertThatThrownBy(() -> usuarios.guardar(odontologo, Seguridad.claveTemporal(), true))
                .hasMessageContaining("colegiatura");
        odontologo.setColegiatura("COP 30552");
        String temporal = Seguridad.claveTemporal();
        usuarios.guardar(odontologo, temporal, true);
        assertThat(temporal).matches("[A-Z][a-z]{3}-\\d{4}");
        assertThat(usuarios.sugerirUsuario("Rubén Mamani")).isEqualTo("ruben.mamani2");

        // Primer ingreso: debe crear su propia contraseña.
        sesion.cerrar();
        Usuario u = usuarios.autenticar("ruben.mamani", temporal);
        assertThat(u.requiereCambioClave()).isTrue();
        assertThatThrownBy(() -> usuarios.crearClavePropia("corta1", "corta1")).hasMessageContaining("8 caracteres");
        usuarios.crearClavePropia("Molar2026", "Molar2026");
        sesion.cerrar();
        assertThat(usuarios.autenticar("ruben.mamani", "Molar2026").requiereCambioClave()).isFalse();
        assertThatThrownBy(() -> usuarios.autenticar("ruben.mamani", temporal)).hasMessage("Usuario o contraseña incorrectos.");

        // El administrador restablece: vuelve a ser temporal.
        sesion.cerrar();
        usuarios.autenticar("ana.rios", "Sonrisa2026");
        String nueva = usuarios.restablecerClave(usuarios.listar().stream()
                .filter(x -> x.getUsuario().equals("ruben.mamani")).findFirst().orElseThrow());
        sesion.cerrar();
        assertThat(usuarios.autenticar("ruben.mamani", nueva).requiereCambioClave()).isTrue();
    }

    @Test
    @Order(6)
    void cadaUnoCambiaSuContrasenaConociendoLaActual() {
        sesion.cerrar();
        usuarios.autenticar("ana.rios", "Sonrisa2026");
        assertThatThrownBy(() -> usuarios.cambiarClave("otra", "Nueva2026x", "Nueva2026x"))
                .hasMessageContaining("actual no es correcta");
        usuarios.cambiarClave("Sonrisa2026", "Nueva2026x", "Nueva2026x");
        sesion.cerrar();
        assertThat(usuarios.autenticar("ana.rios", "Nueva2026x")).isNotNull();
    }
}
