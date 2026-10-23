package com.dentavista.ui;

import com.dentavista.dominio.Odontograma;
import com.dentavista.tresd.VisorDental3D;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * Imágenes del odontograma para el PDF. Se dibujan fuera de pantalla en una escena
 * temporal, así no dependen de la pestaña que el usuario tenga abierta.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Capturas {

    private Capturas() {
    }

    public static Image vista3D(Odontograma o, VisorDental3D.Vista vista, double ancho, double alto) {
        VisorDental3D visor = new VisorDental3D();
        visor.numerosVisiblesProperty().set(false);
        visor.setOdontograma(o);
        visor.ponerVista(vista);
        StackPane fondo = new StackPane(visor);
        fondo.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 80%, #25344f 0%, #0e1625 75%);");
        Scene escena = new Scene(fondo, ancho, alto, true, SceneAntialiasing.BALANCED);
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }

    /** Acercamiento 3D a una pieza (historial de la pieza). */
    public static Image pieza3D(Odontograma o, int fdi, double ancho, double alto) {
        VisorDental3D visor = new VisorDental3D();
        visor.numerosVisiblesProperty().set(false);
        visor.setOdontograma(o);
        visor.ponerEnfoque(fdi);
        visor.seleccionar(fdi);
        StackPane fondo = new StackPane(visor);
        fondo.setStyle("-fx-background-color: radial-gradient(center 50% 45%, radius 80%, #25344f 0%, #0e1625 75%);");
        Scene escena = new Scene(fondo, ancho, alto, true, SceneAntialiasing.BALANCED);
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }

    /** Diagrama de las cinco caras de una pieza en un estado dado (fondo blanco). */
    public static Image diagrama(int fdi, Odontograma.EstadoPieza estado, double tamano) {
        javafx.scene.Group g = Odontograma2D.diagramaPieza(fdi, estado, tamano, null, (p, c) -> { });
        StackPane fondo = new StackPane(g);
        fondo.setStyle("-fx-background-color: white; -fx-padding: 6;");
        Scene escena = new Scene(fondo, Color.WHITE);
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }

    public static Image vista2D(Odontograma o, double ancho, double alto) {
        Odontograma2D o2d = new Odontograma2D();
        o2d.setInteractivo(false);
        o2d.setOdontograma(o);
        StackPane fondo = new StackPane(o2d);
        fondo.setStyle("-fx-background-color: white; -fx-padding: 6;");
        Scene escena = new Scene(fondo, ancho, alto, Color.WHITE);
        escena.getStylesheets().add(Objects.requireNonNull(Capturas.class.getResource("/css/app.css")).toExternalForm());
        fondo.applyCss();
        fondo.layout();
        return escena.snapshot(null);
    }
}
