package com.erejm.dentavista.dominio;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Utilidades de la notacion dental FDI (dos digitos: cuadrante + posicion).
 * <pre>
 *   18 17 16 15 14 13 12 11 | 21 22 23 24 25 26 27 28   (arcada superior)
 *   48 47 46 45 44 43 42 41 | 31 32 33 34 35 36 37 38   (arcada inferior)
 * </pre>
 * El cuadrante 1 es el superior derecho <b>del paciente</b>, que en un odontograma
 * se dibuja a la izquierda de quien lo mira (el odontologo esta frente al paciente).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Pieza {

    /** Fila superior del odontograma, de izquierda a derecha. */
    public static final List<Integer> FILA_SUPERIOR = List.of(
            18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28);

    /** Fila inferior del odontograma, de izquierda a derecha. */
    public static final List<Integer> FILA_INFERIOR = List.of(
            48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38);

    /** Las 32 piezas permanentes en orden de lectura. */
    public static final List<Integer> TODAS = IntStream.concat(
            FILA_SUPERIOR.stream().mapToInt(Integer::intValue),
            FILA_INFERIOR.stream().mapToInt(Integer::intValue)).boxed().toList();

    private static final String[] NOMBRES = {
            "", "Incisivo central", "Incisivo lateral", "Canino", "Primer premolar",
            "Segundo premolar", "Primer molar", "Segundo molar", "Tercer molar"};

    private Pieza() {
    }

    public static boolean esValida(int fdi) {
        int c = fdi / 10;
        int p = fdi % 10;
        return c >= 1 && c <= 4 && p >= 1 && p <= 8;
    }

    public static void validar(int fdi) {
        if (!esValida(fdi)) {
            throw new IllegalArgumentException("Pieza FDI invalida: " + fdi);
        }
    }

    public static int cuadrante(int fdi) {
        return fdi / 10;
    }

    /** 1 = incisivo central ... 8 = tercer molar. */
    public static int posicion(int fdi) {
        return fdi % 10;
    }

    public static boolean esSuperior(int fdi) {
        int c = cuadrante(fdi);
        return c == 1 || c == 2;
    }

    /** Lado derecho del paciente (cuadrantes 1 y 4). */
    public static boolean esDerecha(int fdi) {
        int c = cuadrante(fdi);
        return c == 1 || c == 4;
    }

    /** Incisivos y caninos: su cara masticatoria es un borde (incisal), no una cara oclusal. */
    public static boolean esAnterior(int fdi) {
        return posicion(fdi) <= 3;
    }

    /** Nombre corto: "Primer molar". */
    public static String nombreTipo(int fdi) {
        return NOMBRES[posicion(fdi)];
    }

    /** Nombre completo: "Primer molar superior derecho". */
    public static String nombre(int fdi) {
        return nombreTipo(fdi) + " " + (esSuperior(fdi) ? "superior" : "inferior")
                + " " + (esDerecha(fdi) ? "derecho" : "izquierdo");
    }

    /** Pieza homologa del otro lado (16 -> 26). */
    public static int espejo(int fdi) {
        int c = cuadrante(fdi);
        int otro = switch (c) {
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 4;
            default -> 3;
        };
        return otro * 10 + posicion(fdi);
    }
}
