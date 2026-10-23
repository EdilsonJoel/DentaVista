package com.erejm.dentavista.dominio;

/**
 * Hallazgos clinicos que se registran sobre el odontograma.
 * <p>
 * Cada estado sabe si se aplica a una <b>cara</b> o a la <b>pieza completa</b>, que color
 * lo representa (el mismo en el modelo 3D, el odontograma 2D y el PDF) y, si es una
 * patologia, que tratamiento del catalogo la resuelve.
 * </p>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum EstadoDental {

    SANO("Sano", "#EFE8D8", Alcance.AMBOS, null),
    CARIES("Caries", "#E5484D", Alcance.CARA, "OBT"),
    FRACTURA("Fractura", "#F59E0B", Alcance.CARA, "REC"),
    OBTURADO("Obturación (resina)", "#3B82F6", Alcance.CARA, null),
    SELLANTE("Sellante", "#14B8A6", Alcance.CARA, null),
    ENDODONCIA_INDICADA("Endodoncia indicada", "#C026D3", Alcance.PIEZA, "END"),
    ENDODONCIA("Endodoncia realizada", "#8B5CF6", Alcance.PIEZA, null),
    EXTRACCION_INDICADA("Extracción indicada", "#B91C1C", Alcance.PIEZA, "EXO"),
    CORONA("Corona", "#D4A72C", Alcance.PIEZA, null),
    IMPLANTE("Implante", "#7C8DA6", Alcance.PIEZA, null),
    AUSENTE("Ausente", "#64748B", Alcance.PIEZA, null);

    /** A que se aplica el estado. */
    public enum Alcance { CARA, PIEZA, AMBOS }

    private final String etiqueta;
    private final String color;
    private final Alcance alcance;
    private final String tratamientoSugerido;

    EstadoDental(String etiqueta, String color, Alcance alcance, String tratamientoSugerido) {
        this.etiqueta = etiqueta;
        this.color = color;
        this.alcance = alcance;
        this.tratamientoSugerido = tratamientoSugerido;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Color hexadecimal (#RRGGBB). */
    public String getColor() {
        return color;
    }

    public Alcance getAlcance() {
        return alcance;
    }

    public boolean seAplicaACara() {
        return alcance != Alcance.PIEZA;
    }

    public boolean seAplicaAPieza() {
        return alcance != Alcance.CARA;
    }

    /** Codigo del tratamiento del catalogo que corrige esta patologia (o null). */
    public String getTratamientoSugerido() {
        return tratamientoSugerido;
    }

    /** Requiere tratamiento: entra al plan y al presupuesto. */
    public boolean esPatologia() {
        return tratamientoSugerido != null;
    }

    /**
     * La pieza ya no tiene caras propias que tratar: ausente, implante o corona
     * la cubren por completo.
     */
    public boolean anulaCaras() {
        return this == AUSENTE || this == IMPLANTE || this == CORONA;
    }
}
