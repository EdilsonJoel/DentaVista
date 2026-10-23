package com.erejm.dentavista.dominio;

/**
 * Caras de una pieza dental. {@link #PIEZA} representa la pieza completa
 * (ausente, corona, implante...), no una cara concreta.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum Superficie {

    OCLUSAL("O"),
    VESTIBULAR("V"),
    LINGUAL("L"),
    MESIAL("M"),
    DISTAL("D"),
    PIEZA("");

    /** Las cinco caras clinicas, sin la pieza completa. */
    public static final Superficie[] CARAS = {OCLUSAL, VESTIBULAR, LINGUAL, MESIAL, DISTAL};

    private final String sigla;

    Superficie(String sigla) {
        this.sigla = sigla;
    }

    public boolean esCara() {
        return this != PIEZA;
    }

    /**
     * Nombre clinico segun la pieza: en anteriores la cara oclusal es el borde
     * <i>incisal</i>; en la arcada superior la cara lingual se llama <i>palatina</i>.
     */
    public String etiqueta(int fdi) {
        return switch (this) {
            case OCLUSAL -> Pieza.esAnterior(fdi) ? "Incisal" : "Oclusal";
            case VESTIBULAR -> "Vestibular";
            case LINGUAL -> Pieza.esSuperior(fdi) ? "Palatina" : "Lingual";
            case MESIAL -> "Mesial";
            case DISTAL -> "Distal";
            case PIEZA -> "Pieza completa";
        };
    }

    /** Sigla clinica (O, I, V, P, L, M, D). */
    public String sigla(int fdi) {
        return switch (this) {
            case OCLUSAL -> Pieza.esAnterior(fdi) ? "I" : "O";
            case LINGUAL -> Pieza.esSuperior(fdi) ? "P" : "L";
            default -> sigla;
        };
    }
}
