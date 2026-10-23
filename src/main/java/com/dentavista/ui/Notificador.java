package com.dentavista.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import com.dentavista.servicio.NegocioException;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

/**
 * Retroalimentación visual: notificaciones flotantes, confirmaciones y diálogos con
 * formulario. Los errores de negocio de un formulario se muestran <b>dentro</b> del
 * diálogo, sin cerrarlo, para no perder lo que el usuario escribió.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Slf4j
@Component
public class Notificador {

    /** Acción de un diálogo: si lanza {@link NegocioException} el diálogo sigue abierto. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar();
    }

    private ModalPane modal;
    private VBox zonaToasts;

    public void registrar(ModalPane modal, VBox zonaToasts) {
        this.modal = modal;
        this.zonaToasts = zonaToasts;
    }

    // ------------------------------------------------------------- toasts

    public void exito(String mensaje) {
        toast(mensaje, Feather.CHECK_CIRCLE, Styles.SUCCESS);
    }

    public void info(String mensaje) {
        toast(mensaje, Feather.INFO, Styles.ACCENT);
    }

    public void advertencia(String mensaje) {
        toast(mensaje, Feather.ALERT_TRIANGLE, Styles.WARNING);
    }

    public void error(String mensaje) {
        toast(mensaje, Feather.X_CIRCLE, Styles.DANGER);
    }

    /** Ejecuta una acción y convierte cualquier fallo en un toast de error. */
    public boolean intentar(Accion accion) {
        try {
            accion.ejecutar();
            return true;
        } catch (NegocioException e) {
            advertencia(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado", e);
            error("Ocurrió un error inesperado: " + e.getMessage());
        }
        return false;
    }

    private void toast(String mensaje, Ikon icono, String estilo) {
        if (zonaToasts == null) {
            return;
        }
        Notification n = new Notification(mensaje, new FontIcon(icono));
        n.getStyleClass().addAll(estilo, Styles.ELEVATED_2, "toast");
        n.setPrefWidth(380);
        n.setMaxWidth(380);
        n.setOnClose(e -> cerrarToast(n));
        zonaToasts.getChildren().add(n);
        Animations.fadeInUp(n, Duration.millis(260)).play();
        PauseTransition espera = new PauseTransition(Duration.seconds(3.8));
        espera.setOnFinished(e -> cerrarToast(n));
        espera.play();
    }

    private void cerrarToast(Notification n) {
        if (!zonaToasts.getChildren().contains(n) || n.getProperties().containsKey("cerrando")) {
            return;
        }
        n.getProperties().put("cerrando", true);
        var salida = Animations.fadeOutRight(n, Duration.millis(220));
        salida.setOnFinished(e -> zonaToasts.getChildren().remove(n));
        salida.play();
    }

    // ------------------------------------------------------------- diálogos

    /** Confirmación simple; {@code peligro} pinta el botón de rojo. */
    public void confirmar(String titulo, String mensaje, String textoAccion, boolean peligro, Runnable accion) {
        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        texto.getStyleClass().add("texto-tenue");
        dialogo(peligro ? Feather.ALERT_TRIANGLE : Feather.HELP_CIRCLE, titulo, texto, textoAccion, peligro,
                accion::run, 440);
    }

    /**
     * Diálogo modal con contenido propio. El botón principal ejecuta {@code accion};
     * si termina bien el diálogo se cierra, si lanza {@link NegocioException} el error
     * aparece dentro del diálogo.
     */
    public void dialogo(Ikon icono, String titulo, Node contenido, String textoAccion, boolean peligro,
                        Accion accion, double ancho) {
        FontIcon ic = new FontIcon(icono);
        ic.setIconSize(20);
        // setIconColor y no setStyle: un estilo en línea pisa la fuente de iconos de Ikonli.
        ic.setIconColor(javafx.scene.paint.Color.web(peligro ? "#dc2626" : "#0d9488"));
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dialogo-titulo");
        HBox cab = new HBox(10, ic, lblTitulo);
        cab.setAlignment(Pos.CENTER_LEFT);

        Label error = new Label();
        error.getStyleClass().add("dialogo-error");
        error.setMaxWidth(Double.MAX_VALUE);
        error.setVisible(false);
        error.setManaged(false);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().addAll("secundario", Styles.BUTTON_OUTLINED);
        cancelar.setCancelButton(true);
        cancelar.setOnAction(e -> cerrar());

        Button aceptar = new Button(textoAccion);
        aceptar.getStyleClass().add(peligro ? Styles.DANGER : "primario");
        aceptar.setDefaultButton(!(contenido instanceof javafx.scene.control.TextArea));
        aceptar.setOnAction(e -> {
            try {
                accion.ejecutar();
                cerrar();
            } catch (NegocioException ex) {
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
                Animations.shakeX(error).play();
            } catch (RuntimeException ex) {
                log.error("Error en diálogo", ex);
                error.setText("Error inesperado: " + ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
            }
        });

        Region espacio = new Region();
        HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
        HBox acciones = new HBox(10, espacio, cancelar, aceptar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox caja = new VBox(16, cab, contenido, error, acciones);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(ancho, Region.USE_PREF_SIZE);
        caja.setPrefWidth(ancho);
        mostrar(caja);
    }

    /** Muestra cualquier nodo como capa modal (se cierra con {@link #cerrar()}). */
    public void mostrar(Node nodo) {
        modal.usePredefinedTransitionFactories(null);
        modal.show(nodo);
    }

    public void cerrar() {
        modal.hide(true);
    }
}
