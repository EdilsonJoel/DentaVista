package com.erejm.dentavista.modelo;

/**
 * Roles del consultorio y lo que cada uno puede hacer.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum Rol {

    ADMIN("Administrador"),
    ODONTOLOGO("Odontólogo"),
    ASISTENTE("Asistente"),
    /** Paciente con acceso propio: ve su odontograma y pide citas. */
    PACIENTE("Paciente");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Registra hallazgos en el odontograma y marca tratamientos como realizados. */
    public boolean puedeRegistrarClinica() {
        return this == ADMIN || this == ODONTOLOGO;
    }

    /** Gestiona usuarios del sistema y el catálogo de precios. */
    public boolean puedeAdministrar() {
        return this == ADMIN;
    }

    /** Personal del consultorio (todo menos paciente): ve la lista de pacientes y la agenda. */
    public boolean esPersonal() {
        return this != PACIENTE;
    }
}
