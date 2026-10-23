package com.erejm.dentavista.modelo;

/**
 * Ciclo de vida de una cita.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum EstadoCita {

    SOLICITADA("Por confirmar", "#f59e0b"),
    CONFIRMADA("Confirmada", "#0d9488"),
    ATENDIDA("Atendida", "#2563eb"),
    NO_ASISTIO("No asistió", "#64748b"),
    CANCELADA("Cancelada", "#94a3b8");

    private final String etiqueta;
    private final String color;

    EstadoCita(String etiqueta, String color) {
        this.etiqueta = etiqueta;
        this.color = color;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getColor() {
        return color;
    }

    /** Ocupa el horario (las canceladas liberan el espacio). */
    public boolean ocupaHorario() {
        return this != CANCELADA;
    }

    /** Todavía puede cambiar (confirmarse, cancelarse, atenderse). */
    public boolean abierta() {
        return this == SOLICITADA || this == CONFIRMADA;
    }
}
