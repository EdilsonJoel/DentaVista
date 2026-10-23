package com.dentavista.servicio;

import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Usuario autenticado durante la vida de la aplicación.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
public class SesionService {

    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
    }

    public void cerrar() {
        this.usuario = null;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean haySesion() {
        return usuario != null;
    }

    public String nombre() {
        return usuario == null ? "Sistema" : usuario.getNombre();
    }

    public boolean puedeRegistrarClinica() {
        return usuario != null && usuario.getRol().puedeRegistrarClinica();
    }

    public boolean puedeAdministrar() {
        return usuario != null && usuario.getRol().puedeAdministrar();
    }

    public void exigirClinica() {
        if (!puedeRegistrarClinica()) {
            throw new NegocioException("Tu rol (" + (usuario == null ? "sin sesión" : usuario.getRol().getEtiqueta())
                    + ") no permite modificar la historia clínica. Pide a un odontólogo que lo registre.");
        }
    }

    public boolean esPaciente() {
        return usuario != null && usuario.getRol() == Rol.PACIENTE;
    }

    public boolean esPersonal() {
        return usuario != null && usuario.getRol().esPersonal();
    }

    /** Ficha del paciente que inició sesión (null si es personal del consultorio). */
    public Paciente pacientePropio() {
        return esPaciente() ? usuario.getPaciente() : null;
    }

    /**
     * Un paciente solo puede ver su propia ficha. Sin sesión (procesos internos como
     * los datos de demostración) o con personal del consultorio no hay restricción.
     */
    public void exigirAccesoA(Paciente p) {
        if (esPaciente() && (usuario.getPaciente() == null || p == null
                || !usuario.getPaciente().getId().equals(p.getId()))) {
            throw new NegocioException("Solo puedes ver tu propia ficha.");
        }
    }

    public void exigirPersonal() {
        if (!esPersonal()) {
            throw new NegocioException("Esta sección es solo para el personal del consultorio.");
        }
    }

    public void exigirAdministrador() {
        if (!puedeAdministrar()) {
            throw new NegocioException("Solo un administrador puede realizar esta acción.");
        }
    }
}
