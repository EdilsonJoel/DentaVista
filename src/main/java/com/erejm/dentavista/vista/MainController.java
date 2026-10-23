package com.erejm.dentavista.vista;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.util.Animations;
import com.erejm.dentavista.JavaFxApplication;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Usuario;
import com.erejm.dentavista.servicio.DatosClinica;
import com.erejm.dentavista.servicio.SesionService;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Tema;
import com.erejm.dentavista.ui.Ui;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Panel principal: menú lateral, área de contenido, diálogos y notificaciones.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class MainController implements Navegador {

    private final SesionService sesion;
    private final Tema tema;
    private final Notificador notificador;
    private final DatosClinica clinica;
    private final ObjectProvider<InicioVista> inicio;
    private final ObjectProvider<PacientesVista> pacientes;
    private final ObjectProvider<FichaVista> fichas;
    private final ObjectProvider<CatalogoVista> catalogo;
    private final ObjectProvider<UsuariosVista> usuarios;
    private final ObjectProvider<AgendaVista> agenda;
    private final ObjectProvider<MisCitasVista> misCitas;
    private final com.erejm.dentavista.servicio.CitaService citas;
    private final com.erejm.dentavista.servicio.UsuarioService cuentas;

    @FXML private StackPane raiz;
    @FXML private StackPane contenido;
    @FXML private VBox zonaToasts;
    @FXML private ImageView imgLogo;
    @FXML private ToggleButton navInicio;
    @FXML private ToggleButton navPacientes;
    @FXML private ToggleButton navTratamientos;
    @FXML private ToggleButton navUsuarios;
    @FXML private ToggleButton navAgenda;
    @FXML private ToggleButton navMiOdontograma;
    @FXML private ToggleButton navMisCitas;
    @FXML private Label secConsultorio;
    @FXML private Label secConfiguracion;
    @FXML private HBox tarjetaUsuario;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Label lblClinica;
    @FXML private Button btnTema;
    @FXML private Button btnSalir;

    private final ToggleGroup grupo = new ToggleGroup();
    private Map<Seccion, ToggleButton> botones;
    private boolean navegando;

    @FXML
    private void initialize() {
        ModalPane modal = new ModalPane();
        modal.setId("modal");
        raiz.getChildren().add(1, modal);
        notificador.registrar(modal, zonaToasts);

        var logo = getClass().getResourceAsStream("/img/logo.png");
        if (logo != null) {
            imgLogo.setImage(new Image(logo));
        }

        botones = Map.of(Seccion.INICIO, navInicio, Seccion.PACIENTES, navPacientes, Seccion.AGENDA, navAgenda,
                Seccion.TRATAMIENTOS, navTratamientos, Seccion.USUARIOS, navUsuarios,
                Seccion.MI_ODONTOGRAMA, navMiOdontograma, Seccion.MIS_CITAS, navMisCitas);
        navAgenda.setGraphic(Ui.icono(Feather.CALENDAR, 18));
        navMiOdontograma.setGraphic(Ui.icono(Feather.SMILE, 18));
        navMisCitas.setGraphic(Ui.icono(Feather.CALENDAR, 18));
        navInicio.setGraphic(Ui.icono(Feather.HOME, 18));
        navPacientes.setGraphic(Ui.icono(Feather.USERS, 18));
        navTratamientos.setGraphic(Ui.icono(Feather.LAYERS, 18));
        navUsuarios.setGraphic(Ui.icono(Feather.SHIELD, 18));
        botones.forEach((seccion, b) -> {
            b.setToggleGroup(grupo);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> irA(seccion));
        });
        // Un botón del menú no se puede "deseleccionar" con un segundo clic.
        grupo.selectedToggleProperty().addListener((o, anterior, nuevo) -> {
            if (nuevo == null && anterior != null && !navegando) {
                anterior.setSelected(true);
            }
        });
        // El paciente solo ve su odontograma y sus citas; el personal, el consultorio.
        boolean paciente = sesion.esPaciente();
        mostrarNav(navUsuarios, sesion.puedeAdministrar());
        for (Node n : new Node[]{navInicio, navPacientes, navAgenda, navTratamientos, secConfiguracion}) {
            mostrarNav(n, !paciente);
        }
        mostrarNav(navMiOdontograma, paciente);
        mostrarNav(navMisCitas, paciente);
        secConsultorio.setText(paciente ? "MI SONRISA" : "CONSULTORIO");
        if (!paciente) {
            long porConfirmar = citas.porConfirmar();
            if (porConfirmar > 0) {
                navAgenda.setText("Agenda (" + porConfirmar + ")");
                Ui.tooltip(navAgenda, porConfirmar + " solicitudes de pacientes por confirmar");
            }
        }

        Usuario u = sesion.getUsuario();
        tarjetaUsuario.getChildren().add(0, Ui.avatar(u.iniciales(), u.getNombre(), 36));
        lblUsuario.setText(u.getNombre());
        lblRol.setText(u.getRol().getEtiqueta() + (u.getColegiatura() != null ? " · " + u.getColegiatura() : "")
                + (paciente ? " · DNI " + u.getPaciente().getDni() : ""));
        lblClinica.setText(clinica.nombre());
        tarjetaUsuario.getStyleClass().add("tarjeta-usuario-clic");
        Ui.tooltip(tarjetaUsuario, "Mi cuenta: cambiar contraseña");
        tarjetaUsuario.setOnMouseClicked(e -> miCuenta());

        Runnable iconoTema = () -> {
            boolean oscuro = tema.oscuroProperty().get();
            btnTema.setGraphic(Ui.icono(oscuro ? Feather.SUN : Feather.MOON, 15));
            btnTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
        };
        iconoTema.run();
        tema.oscuroProperty().addListener((o, a, oscuro) -> iconoTema.run());
        Ui.tooltip(btnTema, "Cambiar tema claro / oscuro");
        btnTema.setOnAction(e -> tema.alternar());

        btnSalir.setGraphic(Ui.icono(Feather.LOG_OUT, 15));
        Ui.tooltip(btnSalir, "Cerrar sesión");
        btnSalir.setOnAction(e -> notificador.confirmar("Cerrar sesión",
                "¿Quieres salir de DentaVista? Los cambios ya están guardados.", "Cerrar sesión", false, () -> {
                    sesion.cerrar();
                    JavaFxApplication.get().mostrarLogin();
                }));

        irA(paciente ? Seccion.MI_ODONTOGRAMA : Seccion.INICIO);
    }

    /** Datos de la propia cuenta y cambio de contraseña. */
    private void miCuenta() {
        Usuario u = sesion.getUsuario();
        javafx.scene.layout.HBox quien = new javafx.scene.layout.HBox(12, Ui.avatar(u.iniciales(), u.getNombre(), 44),
                new VBox(2, Ui.etiqueta(u.getNombre(), "texto-fuerte"),
                        Ui.etiqueta("@" + u.getUsuario() + " · " + u.getRol().getEtiqueta(), "texto-tenue")));
        quien.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.PasswordField actual = new javafx.scene.control.PasswordField();
        javafx.scene.control.PasswordField nueva = new javafx.scene.control.PasswordField();
        javafx.scene.control.PasswordField repetir = new javafx.scene.control.PasswordField();
        VBox contenido = new VBox(12, quien, Ui.campo("Contraseña actual", actual), Ui.campo("Nueva contraseña", nueva),
                Ui.campo("Confirmar", repetir), com.erejm.dentavista.ui.ClaveSegura.requisitos(nueva, repetir));
        notificador.dialogo(Feather.USER, "Mi cuenta", contenido, "Cambiar contraseña", false, () -> {
            cuentas.cambiarClave(actual.getText(), nueva.getText(), repetir.getText());
            notificador.exito("Tu contraseña se cambió.");
        }, 460);
    }

    private static void mostrarNav(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(visible);
    }

    @Override
    public void irA(Seccion seccion) {
        marcar(seccion);
        Node vista = switch (seccion) {
            case INICIO -> inicio.getObject().vista();
            case PACIENTES -> pacientes.getObject().vista();
            case TRATAMIENTOS -> catalogo.getObject().vista();
            case USUARIOS -> usuarios.getObject().vista();
            case AGENDA -> agenda.getObject().vista();
            case MIS_CITAS -> misCitas.getObject().vista();
            case MI_ODONTOGRAMA -> fichas.getObject().vista(sesion.pacientePropio());
        };
        mostrar(vista);
    }

    @Override
    public void abrirFicha(Paciente paciente) {
        marcar(sesion.esPaciente() ? Seccion.MI_ODONTOGRAMA : Seccion.PACIENTES);
        mostrar(fichas.getObject().vista(paciente));
    }

    private void marcar(Seccion seccion) {
        navegando = true;
        botones.get(seccion).setSelected(true);
        navegando = false;
    }

    private void mostrar(Node vista) {
        contenido.getChildren().setAll(vista);
        Animations.fadeIn(vista, Duration.millis(220)).play();
    }
}
