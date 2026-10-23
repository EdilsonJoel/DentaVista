package com.erejm.dentavista.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

/**
 * Datos de acceso para entregar a una persona (usuario y contraseña temporal), con botón
 * para copiarlos. Se usa al crear cuentas del personal, al restablecer contraseñas y al
 * dar acceso a un paciente.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Credenciales {

    private Credenciales() {
    }

    /** Muestra el diálogo un instante después, para no chocar con el diálogo que se está cerrando. */
    public static void mostrarDespues(Notificador notificador, String titulo, String nombre, String usuario,
                                      String temporal, String consultorio) {
        PauseTransition p = new PauseTransition(Duration.millis(260));
        p.setOnFinished(e -> mostrar(notificador, titulo, nombre, usuario, temporal, consultorio));
        p.play();
    }

    public static void mostrar(Notificador notificador, String titulo, String nombre, String usuario, String temporal,
                               String consultorio) {
        Label t = Ui.etiqueta(titulo, "dialogo-titulo");
        HBox cab = new HBox(10, Ui.icono(Feather.CHECK_CIRCLE, 20), t);
        cab.setAlignment(Pos.CENTER_LEFT);
        Label texto = Ui.etiqueta("Entrega estos datos a " + nombre + ". Al ingresar por primera vez se le pedirá crear "
                + "su propia contraseña; la temporal deja de funcionar en ese momento.", "texto-tenue");
        texto.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(6);
        g.add(Ui.etiqueta("USUARIO", "campo-rotulo"), 0, 0);
        g.add(Ui.etiqueta(usuario, "credencial-valor"), 0, 1);
        g.add(Ui.etiqueta("CONTRASEÑA TEMPORAL", "campo-rotulo"), 1, 0);
        g.add(Ui.etiqueta(temporal, "credencial-valor"), 1, 1);
        g.getStyleClass().add("credencial");

        Button copiar = Ui.secundario("Copiar", Feather.COPY);
        copiar.setOnAction(e -> {
            ClipboardContent c = new ClipboardContent();
            c.putString("DentaVista · " + consultorio + "\nUsuario: " + usuario + "\nContraseña temporal: " + temporal);
            Clipboard.getSystemClipboard().setContent(c);
            notificador.exito("Datos copiados: pégalos en un mensaje para " + nombre.split(" ")[0] + ".");
        });
        Button listo = Ui.primario("Listo", Feather.CHECK);
        listo.setDefaultButton(true);
        listo.setOnAction(e -> notificador.cerrar());
        HBox acciones = new HBox(10, Ui.espaciador(), copiar, listo);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox caja = new VBox(16, cab, texto, g, acciones);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(520, Region.USE_PREF_SIZE);
        caja.setPrefWidth(520);
        notificador.mostrar(caja);
    }
}
