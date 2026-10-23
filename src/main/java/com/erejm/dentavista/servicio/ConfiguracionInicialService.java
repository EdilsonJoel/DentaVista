package com.erejm.dentavista.servicio;

import com.erejm.dentavista.datos.DatosDemo;
import com.erejm.dentavista.modelo.Consultorio;
import com.erejm.dentavista.modelo.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Primer uso de DentaVista: registra el consultorio y su administrador, crea el
 * catálogo base de tratamientos y, si se pide, carga pacientes de ejemplo.
 * Todo o nada: si algo falla no queda una configuración a medias.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionInicialService {

    /** Datos de la cuenta del administrador que llena el asistente. */
    public record Administrador(String nombre, String usuario, String email, String clave, String confirmacion) {
    }

    private final UsuarioService usuarios;
    private final DatosClinica clinica;
    private final DatosDemo datos;

    /** No hay ningún administrador activo: hay que configurar el consultorio. */
    public boolean requiereConfiguracion() {
        return !usuarios.hayAdministrador();
    }

    @Transactional
    public Usuario configurar(Consultorio consultorio, Administrador admin, boolean ejemplos) {
        if (!requiereConfiguracion()) {
            throw new NegocioException("DentaVista ya está configurado. Ingresa con tu cuenta.");
        }
        try {
            clinica.guardar(consultorio);
            Usuario cuenta = usuarios.crearAdministradorInicial(admin.nombre(), admin.usuario(), admin.email(),
                    admin.clave(), admin.confirmacion());
            datos.cargarCatalogoBase();
            if (ejemplos) {
                datos.cargarEjemplos();
            }
            return cuenta;
        } catch (RuntimeException e) {
            clinica.recargar(); // la transacción se deshace: que no quede el consultorio en memoria
            throw e;
        }
    }
}
