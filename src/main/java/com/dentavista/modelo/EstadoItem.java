package com.dentavista.modelo;

/**
 * Ciclo de vida de un tratamiento del plan.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum EstadoItem {

    PENDIENTE("Por realizar"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
