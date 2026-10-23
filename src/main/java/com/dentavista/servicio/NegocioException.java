package com.dentavista.servicio;

/**
 * Regla de negocio incumplida. El mensaje está pensado para mostrarse tal cual
 * al usuario: dice qué pasó y, cuando se puede, qué hacer.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
