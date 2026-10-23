package com.erejm.dentavista.modelo;

/**
 * Medios de pago habituales en un consultorio peruano.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum MedioPago {

    EFECTIVO("Efectivo"),
    YAPE("Yape"),
    PLIN("Plin"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String etiqueta;

    MedioPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
