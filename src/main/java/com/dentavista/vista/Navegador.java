package com.dentavista.vista;

import com.dentavista.modelo.Paciente;

/**
 * Navegación entre pantallas del panel principal.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public interface Navegador {

    enum Seccion { INICIO, PACIENTES, AGENDA, TRATAMIENTOS, USUARIOS, MI_ODONTOGRAMA, MIS_CITAS }

    void irA(Seccion seccion);

    void abrirFicha(Paciente paciente);
}
