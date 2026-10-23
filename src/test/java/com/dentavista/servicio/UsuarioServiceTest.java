package com.dentavista.servicio;

import com.dentavista.IntegracionBase;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Acceso y gestión de usuarios.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
class UsuarioServiceTest extends IntegracionBase {

    @Autowired
    private UsuarioService servicio;

    @Test
    void laContrasenaSeGuardaConHashYSal() {
        String a = Seguridad.hashear("secreta");
        String b = Seguridad.hashear("secreta");
        assertThat(a).isNotEqualTo(b).doesNotContain("secreta");
        assertThat(Seguridad.coincide("secreta", a)).isTrue();
        assertThat(Seguridad.coincide("otra", a)).isFalse();
    }

    @Test
    void autenticaConCredencialesCorrectas() {
        Usuario u = iniciarSesion(Rol.ODONTOLOGO);
        sesion.cerrar();
        Usuario ok = servicio.autenticar(u.getUsuario(), "clave123");
        assertThat(ok.getUltimoAcceso()).isNotNull();
        assertThat(sesion.getUsuario()).isEqualTo(ok);
    }

    @Test
    void rechazaContrasenaIncorrectaSinRevelarSiElUsuarioExiste() {
        Usuario u = iniciarSesion(Rol.ODONTOLOGO);
        assertThatThrownBy(() -> servicio.autenticar(u.getUsuario(), "mala"))
                .hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> servicio.autenticar("no.existe", "mala"))
                .hasMessage("Usuario o contraseña incorrectos.");
    }

    @Test
    void soloElAdministradorCreaUsuarios() {
        iniciarSesion(Rol.ODONTOLOGO);
        Usuario nuevo = Usuario.builder().usuario("nuevo.doc").nombre("Nuevo").colegiatura("COP 11111").rol(Rol.ODONTOLOGO).build();
        assertThatThrownBy(() -> servicio.guardar(nuevo, "clave123")).isInstanceOf(NegocioException.class);

        iniciarSesion(Rol.ADMIN);
        assertThat(servicio.guardar(nuevo, "clave123").getId()).isNotNull();
    }
}
