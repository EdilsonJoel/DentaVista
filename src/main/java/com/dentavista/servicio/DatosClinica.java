package com.dentavista.servicio;

import com.dentavista.modelo.Consultorio;
import com.dentavista.repositorio.ConsultorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Datos del consultorio (nombre, RUC, dirección, contacto) que aparecen en la interfaz
 * y en los PDF. Se guardan en la base de datos: los registra el asistente de primer uso
 * y luego los edita el administrador.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
public class DatosClinica {

    private final ConsultorioRepository repositorio;
    private final Validacion validacion;
    private final SesionService sesion;

    private volatile Consultorio cache;

    @Transactional(readOnly = true)
    public Consultorio actual() {
        if (cache == null) {
            cache = repositorio.findFirstByOrderByIdAsc().orElse(null);
        }
        return cache;
    }

    /** Vuelve a leer los datos de la base en el próximo uso. */
    public void recargar() {
        cache = null;
    }

    public boolean configurado() {
        return actual() != null;
    }

    public String nombre() {
        Consultorio c = actual();
        return c == null ? "Consultorio dental" : c.getNombre();
    }

    public String ruc() {
        return valor(actual() == null ? null : actual().getRuc());
    }

    public String direccion() {
        return valor(actual() == null ? null : actual().getDireccion());
    }

    public String telefono() {
        return valor(actual() == null ? null : actual().getTelefono());
    }

    public String email() {
        return valor(actual() == null ? null : actual().getEmail());
    }

    /** "RUC 20605418731 · Jr. Lima 452" (omite lo que no se registró). */
    public String lineaLegal() {
        return unir(ruc().isEmpty() ? "" : "RUC " + ruc(), direccion());
    }

    /** "(051) 32-4580 · citas@clinica.pe" (omite lo que no se registró). */
    public String lineaContacto() {
        return unir(telefono(), email());
    }

    /**
     * Registra o actualiza los datos. La primera vez lo hace el asistente de primer uso;
     * después, solo el administrador.
     */
    @Transactional
    public Consultorio guardar(Consultorio datos) {
        if (configurado()) {
            sesion.exigirAdministrador();
        }
        Consultorio c = actual() != null ? repositorio.findById(actual().getId()).orElseGet(Consultorio::new) : new Consultorio();
        c.setNombre(limpio(datos.getNombre()));
        c.setRuc(limpio(datos.getRuc()));
        c.setDireccion(limpio(datos.getDireccion()));
        c.setTelefono(limpio(datos.getTelefono()));
        c.setEmail(limpio(datos.getEmail()) == null ? null : limpio(datos.getEmail()).toLowerCase());
        validacion.validar(c);
        cache = repositorio.save(c);
        return cache;
    }

    private static String valor(String s) {
        return s == null ? "" : s;
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim().replaceAll("\\s+", " ");
    }

    private static String unir(String... partes) {
        return Stream.of(partes).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(" · "));
    }
}
