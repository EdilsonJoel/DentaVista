package com.dentavista.servicio;

import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Cuentas de DentaVista: inicio de sesión, personal del consultorio (lo gestiona el
 * administrador), acceso de los pacientes y cambio de contraseña.
 * <p>
 * Las cuentas nuevas reciben una contraseña temporal y deben cambiarla en su primer
 * ingreso; así el administrador nunca conoce la contraseña definitiva de nadie.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private static final Set<String> TITULOS = Set.of("dr", "dra", "lic", "mg", "cd", "od", "sr", "sra", "srta");
    private static final Set<String> CONECTORES = Set.of("de", "del", "la", "las", "los", "y");

    private final UsuarioRepository usuarios;
    private final SesionService sesion;
    private final Validacion validacion;

    // ----------------------------------------------------------------- acceso

    /** Verifica credenciales; el mismo mensaje para usuario o contraseña incorrectos. */
    public Usuario autenticar(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isEmpty()) {
            throw new NegocioException("Escribe tu usuario y contraseña.");
        }
        Usuario u = usuarios.findByUsuarioIgnoreCase(usuario.trim())
                .filter(x -> Seguridad.coincide(contrasena, x.getContrasena()))
                .orElseThrow(() -> new NegocioException("Usuario o contraseña incorrectos."));
        if (u.getRol() == Rol.PACIENTE && u.getPaciente() == null) {
            throw new NegocioException("Este acceso de paciente no está vinculado a ninguna ficha.");
        }
        if (!u.isActivo()) {
            throw new NegocioException("Tu cuenta está desactivada. Habla con el administrador del consultorio.");
        }
        u.setUltimoAcceso(LocalDateTime.now());
        Usuario guardado = usuarios.save(u);
        sesion.iniciar(guardado);
        return guardado;
    }

    /** Hay al menos un administrador activo (si no, DentaVista pide la configuración inicial). */
    @Transactional(readOnly = true)
    public boolean hayAdministrador() {
        return usuarios.countByRolAndActivoTrue(Rol.ADMIN) > 0;
    }

    // ---------------------------------------------------------------- personal

    /** Personal del consultorio (los accesos de pacientes se gestionan desde su ficha). */
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findByRolNotOrderByNombreAsc(Rol.PACIENTE);
    }

    /**
     * Usuario sugerido a partir del nombre: "Dra. Lucía Paredes Quispe" → "lucia.paredes".
     * Si ya existe se le agrega un número.
     */
    @Transactional(readOnly = true)
    public String sugerirUsuario(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "";
        }
        String limpio = Normalizer.normalize(nombreCompleto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s]", " ");
        List<String> palabras = Arrays.stream(limpio.trim().split("\\s+"))
                .filter(p -> !p.isBlank() && !TITULOS.contains(p) && !CONECTORES.contains(p)).toList();
        if (palabras.isEmpty()) {
            return "";
        }
        // Nombre + primer apellido (en "nombre nombre apellido apellido" es el penúltimo).
        String apellido = palabras.size() >= 4 ? palabras.get(palabras.size() - 2)
                : palabras.size() >= 2 ? palabras.get(1) : "";
        String base = (palabras.get(0) + (apellido.isEmpty() ? "" : "." + apellido));
        base = base.substring(0, Math.min(base.length(), 26));
        String candidato = base;
        for (int n = 2; usuarios.findByUsuarioIgnoreCase(candidato).isPresent(); n++) {
            candidato = base + n;
        }
        return candidato;
    }

    /** Crea o actualiza una cuenta del personal (sin contraseña nueva al editar). */
    public Usuario guardar(Usuario u, String contrasena) {
        return guardar(u, contrasena, false);
    }

    /**
     * Crea o actualiza una cuenta del personal.
     *
     * @param contrasena    obligatoria al crear; al editar, vacía = se conserva la actual
     * @param debeCambiarla la cuenta nueva deberá crear su propia contraseña al ingresar
     */
    public Usuario guardar(Usuario u, String contrasena, boolean debeCambiarla) {
        sesion.exigirAdministrador();
        boolean nuevo = u.getId() == null;
        if (u.getRol() == null || u.getRol() == Rol.PACIENTE) {
            throw new NegocioException("Elige el rol: administrador, odontólogo o asistente.");
        }
        u.setUsuario(u.getUsuario() == null ? null : u.getUsuario().trim().toLowerCase(Locale.ROOT));
        u.setNombre(u.getNombre() == null ? null : u.getNombre().trim().replaceAll("\\s+", " "));
        u.setColegiatura(vacioANulo(u.getColegiatura()));
        u.setEmail(vacioANulo(u.getEmail()));
        if (u.getRol() == Rol.ODONTOLOGO && u.getColegiatura() == null) {
            throw new NegocioException("Ingresa el número de colegiatura (COP) del odontólogo: aparece en recetas e informes.");
        }
        if (nuevo || (contrasena != null && !contrasena.isEmpty())) {
            Seguridad.validarClave(contrasena);
        }
        usuarios.findByUsuarioIgnoreCase(u.getUsuario() == null ? "" : u.getUsuario())
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new NegocioException("El usuario \"" + x.getUsuario() + "\" ya existe. Prueba con otro.");
                });
        if (!nuevo) {
            Usuario actual = usuarios.findById(u.getId()).orElseThrow();
            boolean dejaSinAdmin = actual.getRol() == Rol.ADMIN && actual.isActivo()
                    && (u.getRol() != Rol.ADMIN || !u.isActivo())
                    && usuarios.countByRolAndActivoTrue(Rol.ADMIN) <= 1;
            if (dejaSinAdmin) {
                throw new NegocioException("Debe quedar al menos un administrador activo.");
            }
            if (sesion.getUsuario() != null && actual.getId().equals(sesion.getUsuario().getId()) && !u.isActivo()) {
                throw new NegocioException("No puedes desactivar tu propia cuenta.");
            }
            u.setContrasena(actual.getContrasena());
            u.setDebeCambiarClave(actual.getDebeCambiarClave());
        } else {
            u.setDebeCambiarClave(debeCambiarla);
        }
        if (contrasena != null && !contrasena.isEmpty()) {
            u.setContrasena(Seguridad.hashear(contrasena));
        }
        validacion.validar(u);
        return usuarios.save(u);
    }

    /**
     * El administrador restablece la contraseña de una cuenta: se genera una temporal que
     * la persona deberá cambiar al ingresar. Devuelve la temporal para entregársela.
     */
    public String restablecerClave(Usuario u) {
        sesion.exigirAdministrador();
        Usuario actual = usuarios.findById(u.getId())
                .orElseThrow(() -> new NegocioException("La cuenta ya no existe."));
        String temporal = Seguridad.claveTemporal();
        actual.setContrasena(Seguridad.hashear(temporal));
        actual.setDebeCambiarClave(true);
        actual.setActivo(true);
        usuarios.save(actual);
        return temporal;
    }

    /** Primera cuenta del sistema, creada por el asistente de primer uso. */
    public Usuario crearAdministradorInicial(String nombre, String usuario, String email, String contrasena,
                                             String confirmacion) {
        if (hayAdministrador()) {
            throw new NegocioException("DentaVista ya tiene un administrador.");
        }
        if (contrasena == null || !contrasena.equals(confirmacion)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        Seguridad.validarClave(contrasena);
        String login = usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByUsuarioIgnoreCase(login).isPresent()) {
            throw new NegocioException("El usuario \"" + login + "\" ya existe. Prueba con otro.");
        }
        Usuario u = Usuario.builder()
                .usuario(login)
                .nombre(nombre == null ? null : nombre.trim().replaceAll("\\s+", " "))
                .email(vacioANulo(email))
                .contrasena(Seguridad.hashear(contrasena))
                .rol(Rol.ADMIN)
                .activo(true)
                .debeCambiarClave(false)
                .build();
        validacion.validar(u);
        return usuarios.save(u);
    }

    // ------------------------------------------------------------- contraseña

    /** Cambio de contraseña desde "Mi cuenta": pide la actual. */
    public void cambiarClave(String actual, String nueva, String confirmacion) {
        Usuario u = usuarioEnSesion();
        if (actual == null || !Seguridad.coincide(actual, u.getContrasena())) {
            throw new NegocioException("La contraseña actual no es correcta.");
        }
        fijarClave(u, nueva, confirmacion);
    }

    /**
     * Primer ingreso con contraseña temporal: la persona crea la suya (ya demostró conocer
     * la temporal al iniciar sesión).
     */
    public void crearClavePropia(String nueva, String confirmacion) {
        Usuario u = usuarioEnSesion();
        if (!u.requiereCambioClave()) {
            throw new NegocioException("Tu cuenta no tiene una contraseña temporal.");
        }
        fijarClave(u, nueva, confirmacion);
    }

    private void fijarClave(Usuario u, String nueva, String confirmacion) {
        if (nueva == null || !nueva.equals(confirmacion)) {
            throw new NegocioException("Las contraseñas no coinciden.");
        }
        Seguridad.validarClave(nueva);
        if (Seguridad.coincide(nueva, u.getContrasena())) {
            throw new NegocioException("La nueva contraseña debe ser distinta de la anterior.");
        }
        u.setContrasena(Seguridad.hashear(nueva));
        u.setDebeCambiarClave(false);
        sesion.iniciar(usuarios.save(u));
    }

    private Usuario usuarioEnSesion() {
        if (!sesion.haySesion()) {
            throw new NegocioException("Inicia sesión primero.");
        }
        return usuarios.findById(sesion.getUsuario().getId()).orElseThrow();
    }

    // -------------------------------------------------------------- pacientes

    @Transactional(readOnly = true)
    public Optional<Usuario> accesoDe(Paciente p) {
        return usuarios.findByPaciente(p);
    }

    /**
     * Crea (o renueva la contraseña de) el acceso de un paciente. Lo hace el personal del
     * consultorio, por ejemplo en recepción; el usuario por defecto es el DNI.
     */
    public Usuario crearAccesoPaciente(Paciente p, String login, String contrasena) {
        return crearAccesoPaciente(p, login, contrasena, false);
    }

    /** Igual, indicando si la contraseña es temporal (el paciente creará la suya al ingresar). */
    public Usuario crearAccesoPaciente(Paciente p, String login, String contrasena, boolean temporal) {
        sesion.exigirPersonal();
        Seguridad.validarClave(contrasena);
        String l = login == null || login.isBlank() ? p.getDni() : login.trim();
        Usuario u = usuarios.findByPaciente(p).orElseGet(() -> Usuario.builder()
                .rol(Rol.PACIENTE).paciente(p).activo(true).build());
        usuarios.findByUsuarioIgnoreCase(l)
                .filter(x -> !x.getId().equals(u.getId()))
                .ifPresent(x -> {
                    throw new NegocioException("El usuario \"" + l + "\" ya existe.");
                });
        u.setUsuario(l);
        u.setNombre(p.nombreCompleto());
        u.setEmail(p.getEmail());
        u.setContrasena(Seguridad.hashear(contrasena));
        u.setDebeCambiarClave(temporal);
        u.setActivo(true);
        validacion.validar(u);
        return usuarios.save(u);
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
