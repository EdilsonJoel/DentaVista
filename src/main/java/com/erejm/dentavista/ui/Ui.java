package com.erejm.dentavista.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Pequeña fábrica de componentes con el estilo de la aplicación.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Ui {

    /** Colores de avatar (se elige uno estable por nombre). */
    private static final String[] COLORES_AVATAR = {
            "#0d9488", "#2563eb", "#7c3aed", "#db2777", "#ea580c", "#0891b2", "#4f46e5", "#16a34a"};

    private Ui() {
    }

    public static FontIcon icono(Ikon ikon, int tamano) {
        FontIcon i = new FontIcon(ikon);
        i.setIconSize(tamano);
        return i;
    }

    public static Button primario(String texto, Ikon ikon) {
        Button b = new Button(texto, ikon == null ? null : icono(ikon, 15));
        b.getStyleClass().add("primario");
        return b;
    }

    public static Button secundario(String texto, Ikon ikon) {
        Button b = new Button(texto, ikon == null ? null : icono(ikon, 15));
        b.getStyleClass().addAll("secundario", "button-outlined");
        return b;
    }

    public static Button botonIcono(Ikon ikon, String ayuda) {
        Button b = new Button(null, icono(ikon, 16));
        b.getStyleClass().addAll("icono", "button-icon", "flat");
        tooltip(b, ayuda);
        return b;
    }

    public static void tooltip(Node n, String texto) {
        Tooltip t = new Tooltip(texto);
        t.setShowDelay(Duration.millis(250));
        Tooltip.install(n, t);
    }

    public static Label etiqueta(String texto, String... clases) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(clases);
        return l;
    }

    public static Label chip(String texto, String clase, Ikon ikon) {
        Label l = new Label(texto, ikon == null ? null : icono(ikon, 12));
        l.getStyleClass().addAll("chip", clase);
        return l;
    }

    public static VBox tarjeta(Node... hijos) {
        VBox v = new VBox(hijos);
        v.getStyleClass().add("tarjeta");
        return v;
    }

    /** Cabecera de tarjeta: título + subtítulo a la izquierda y acciones a la derecha. */
    public static HBox cabecera(String titulo, String subtitulo, Node... acciones) {
        VBox textos = new VBox(2, etiqueta(titulo, "tarjeta-titulo"));
        if (subtitulo != null) {
            textos.getChildren().add(etiqueta(subtitulo, "tarjeta-subtitulo"));
        }
        HBox h = new HBox(8, textos, espaciador());
        h.getChildren().addAll(acciones);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /**
     * Restringe un campo a solo dígitos y a un máximo de caracteres (p. ej. DNI = 8):
     * lo que no cumple simplemente no se escribe, ni al teclear ni al pegar.
     */
    public static void soloDigitos(javafx.scene.control.TextInputControl campo, int maximo) {
        campo.setTextFormatter(new javafx.scene.control.TextFormatter<String>(cambio -> {
            // Se quitan los caracteres que no son dígitos y se recorta lo que exceda el máximo.
            String nuevo = cambio.getText().replaceAll("\\D", "");
            int reemplazado = cambio.getRangeEnd() - cambio.getRangeStart();
            int libre = Math.max(0, maximo - (cambio.getControlText().length() - reemplazado));
            if (nuevo.length() > libre) {
                nuevo = nuevo.substring(0, libre);
            }
            cambio.setText(nuevo);
            int caret = cambio.getRangeStart() + nuevo.length();
            cambio.selectRange(caret, caret);
            return cambio;
        }));
    }

    public static Region espaciador() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static StackPane avatar(String iniciales, String semilla, double tamano) {
        Label l = new Label(iniciales);
        l.setStyle("-fx-font-size: " + Math.round(tamano * 0.38) + "px;");
        StackPane s = new StackPane(l);
        s.getStyleClass().add("avatar");
        String color = COLORES_AVATAR[Math.floorMod(semilla == null ? 0 : semilla.hashCode(), COLORES_AVATAR.length)];
        s.setStyle("-fx-background-color: linear-gradient(to bottom right, " + color + ", derive(" + color + ", -25%));");
        s.setMinSize(tamano, tamano);
        s.setMaxSize(tamano, tamano);
        return s;
    }

    /** Campo de formulario: rótulo arriba, control debajo. */
    public static VBox campo(String rotulo, Node control) {
        Label l = etiqueta(rotulo, "campo-rotulo");
        VBox v = new VBox(5, l, control);
        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return v;
    }

    /** Estado vacío: icono grande + mensaje. */
    public static VBox vacio(Ikon ikon, String titulo, String detalle) {
        Label t = etiqueta(titulo, "tarjeta-titulo");
        Label d = etiqueta(detalle, "texto-tenue");
        d.setWrapText(true);
        d.setMaxWidth(320);
        d.setStyle("-fx-text-alignment: center;");
        VBox v = new VBox(8, icono(ikon, 36), t, d);
        v.getStyleClass().add("vacio");
        return v;
    }

    /** Cuadrito de color para leyendas y paletas. */
    public static Region muestra(String colorHex) {
        Region r = new Region();
        r.getStyleClass().add("muestra-color");
        r.setStyle("-fx-background-color: " + colorHex + ";");
        return r;
    }
}
