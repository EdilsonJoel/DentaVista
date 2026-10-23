package com.erejm.dentavista.vista;

import atlantafx.base.util.Animations;
import com.erejm.dentavista.JavaFxApplication;
import com.erejm.dentavista.modelo.Usuario;
import com.erejm.dentavista.servicio.DatosClinica;
import com.erejm.dentavista.servicio.NegocioException;
import com.erejm.dentavista.servicio.SesionService;
import com.erejm.dentavista.servicio.UsuarioService;
import com.erejm.dentavista.tresd.VisorDental3D;
import com.erejm.dentavista.ui.ClaveSegura;
import com.erejm.dentavista.ui.Ui;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/**
 * Pantalla de acceso con la dentadura 3D girando como presentación del producto.
 * Si la cuenta tiene una contraseña temporal (la asignó el administrador), antes de
 * entrar se pide crear una propia.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioService usuarios;
    private final SesionService sesion;
    private final DatosClinica clinica;

    @FXML private StackPane escenario;
    @FXML private VBox puntos;
    @FXML private VBox formulario;
    @FXML private Label lblClinica;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtClave;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;

    private VisorDental3D visor;

    @FXML
    private void initialize() {
        lblClinica.setText("Ingresa a " + clinica.nombre() + ".");

        visor = new VisorDental3D();
        visor.numerosVisiblesProperty().set(false);
        visor.giroAutomaticoProperty().set(true);
        visor.setMouseTransparent(false);
        // El modelo deja libre la franja del título y la de los puntos destacados.
        StackPane.setMargin(visor, new javafx.geometry.Insets(120, 0, 150, 0));
        escenario.getChildren().add(0, visor);

        puntos.getChildren().addAll(
                punto(Feather.MOUSE_POINTER, "Odontograma 3D: clic en la cara exacta del diente"),
                punto(Feather.CALENDAR, "Agenda de citas y recordatorios por WhatsApp"),
                punto(Feather.DOLLAR_SIGN, "Presupuestos, pagos, recibos y recetas en PDF"),
                punto(Feather.USERS, "Tus pacientes ven su odontograma y piden citas"));
    }

    private HBox punto(Feather icono, String texto) {
        Label l = new Label(texto, Ui.icono(icono, 15));
        l.getStyleClass().add("login-punto");
        l.setGraphicTextGap(10);
        return new HBox(l);
    }

    public void alMostrar() {
        txtUsuario.requestFocus();
    }

    /** Deja el usuario escrito (por ejemplo, después de configurar el consultorio). */
    public void sugerirUsuario(String usuario) {
        txtUsuario.setText(usuario);
        txtClave.requestFocus();
    }

    @FXML
    private void ingresar() {
        try {
            Usuario u = usuarios.autenticar(txtUsuario.getText(), txtClave.getText());
            if (u.requiereCambioClave()) {
                pedirClavePropia(u);
                return;
            }
            entrar();
        } catch (NegocioException e) {
            mostrarError(lblError, btnIngresar, e.getMessage());
            txtClave.selectAll();
            txtClave.requestFocus();
        }
    }

    private void entrar() {
        visor.giroAutomaticoProperty().set(false);
        JavaFxApplication.get().mostrarPanel();
    }

    /** Primer ingreso con contraseña temporal: la persona crea la suya antes de entrar. */
    private void pedirClavePropia(Usuario u) {
        Label titulo = Ui.etiqueta("Crea tu contraseña", "login-bienvenida");
        Label texto = Ui.etiqueta("Hola, " + u.getNombre().split(" ")[0] + ". Ingresaste con una contraseña temporal; "
                + "crea una propia para continuar. Nadie más la conocerá.", "texto-tenue");
        texto.setWrapText(true);
        PasswordField nueva = new PasswordField();
        nueva.getStyleClass().add("campo-grande");
        nueva.setPromptText("Nueva contraseña");
        PasswordField repetir = new PasswordField();
        repetir.getStyleClass().add("campo-grande");
        repetir.setPromptText("Repite la contraseña");
        Label error = new Label();
        error.getStyleClass().add("login-error");
        error.setVisible(false);
        error.setManaged(false);
        Button guardar = Ui.primario("Guardar y entrar", Feather.CHECK);
        guardar.getStyleClass().add("boton-grande");
        guardar.setMaxWidth(Double.MAX_VALUE);
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> {
            try {
                usuarios.crearClavePropia(nueva.getText(), repetir.getText());
                entrar();
            } catch (NegocioException ex) {
                mostrarError(error, guardar, ex.getMessage());
            }
        });
        Button volver = Ui.secundario("Volver", Feather.ARROW_LEFT);
        volver.setOnAction(e -> {
            sesion.cerrar();
            JavaFxApplication.get().mostrarLogin();
        });
        Region esp = new Region();
        esp.setMinHeight(6);
        formulario.getChildren().setAll(titulo, texto, esp,
                Ui.etiqueta("NUEVA CONTRASEÑA", "campo-rotulo"), nueva,
                Ui.etiqueta("CONFIRMAR", "campo-rotulo"), repetir,
                ClaveSegura.requisitos(nueva, repetir), error, guardar, volver);
        nueva.requestFocus();
    }

    private static void mostrarError(Label etiqueta, Button boton, String mensaje) {
        etiqueta.setText(mensaje);
        etiqueta.setVisible(true);
        etiqueta.setManaged(true);
        Animations.shakeX(boton).play();
    }
}
