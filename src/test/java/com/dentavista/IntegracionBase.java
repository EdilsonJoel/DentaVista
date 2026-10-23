package com.dentavista;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Tratamiento;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.PacienteRepository;
import com.dentavista.repositorio.TratamientoRepository;
import com.dentavista.repositorio.UsuarioRepository;
import com.dentavista.servicio.Seguridad;
import com.dentavista.servicio.SesionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base de las pruebas de integración: contexto completo de Spring sobre una base
 * SQLite temporal y sin datos de demostración.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@SpringBootTest
public abstract class IntegracionBase {

    private static final AtomicInteger DNI = new AtomicInteger(10000000);

    @Autowired protected PacienteRepository pacientes;
    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected TratamientoRepository tratamientos;
    @Autowired protected SesionService sesion;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        Path carpeta = Files.createTempDirectory("dentavista-test");
        carpeta.toFile().deleteOnExit();
        registro.add("spring.datasource.url", () -> "jdbc:sqlite:" + carpeta.resolve("prueba.db"));
        registro.add("app.demo.enabled", () -> "false");
        registro.add("logging.level.com.dentavista", () -> "WARN");
    }

    protected Usuario iniciarSesion(Rol rol) {
        String login = rol.name().toLowerCase() + DNI.incrementAndGet();
        Usuario u = usuarios.save(Usuario.builder()
                .usuario(login)
                .contrasena(Seguridad.hashear("clave123"))
                .nombre("Usuario " + rol.getEtiqueta())
                .rol(rol)
                .activo(true)
                .build());
        sesion.iniciar(u);
        return u;
    }

    protected Paciente pacienteNuevo() {
        return pacientes.save(Paciente.builder()
                .dni(String.valueOf(DNI.incrementAndGet()))
                .nombres("Paciente")
                .apellidos("De Prueba")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .build());
    }

    protected Tratamiento tratamiento(String codigo, String precio, EstadoDental resultado, boolean porCara) {
        // El contexto (y la base) se comparte entre pruebas: se deja siempre en su estado inicial.
        Tratamiento t = tratamientos.findByCodigo(codigo).orElseGet(Tratamiento::new);
        t.setCodigo(codigo);
        t.setNombre("Tratamiento " + codigo);
        t.setPrecio(new BigDecimal(precio));
        t.setEstadoResultante(resultado);
        t.setPorCara(porCara);
        t.setRequierePieza(true);
        t.setActivo(true);
        return tratamientos.save(t);
    }
}
