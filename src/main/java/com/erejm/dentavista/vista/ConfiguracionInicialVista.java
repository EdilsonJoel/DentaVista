package com.erejm.dentavista.vista;

import atlantafx.base.util.Animations;
import com.erejm.dentavista.JavaFxApplication;
import com.erejm.dentavista.modelo.Consultorio;
import com.erejm.dentavista.servicio.ConfiguracionInicialService;
import com.erejm.dentavista.servicio.NegocioException;
import com.erejm.dentavista.servicio.UsuarioService;
import com.erejm.dentavista.tresd.VisorDental3D;
import com.erejm.dentavista.ui.ClaveSegura;
import com.erejm.dentavista.ui.Mascaras;
import com.erejm.dentavista.ui.Ui;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Asistente de primer uso. La primera vez que se abre DentaVista (sin administrador)
 * se registran en dos pasos los datos del consultorio y la cuenta del administrador.
 * Opcionalmente se cargan pacientes de ejemplo para conocer el sistema.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ConfiguracionInicialVista {

    private final ConfiguracionInicialService configuracion;
    private final UsuarioService usuarios;

    private final VBox panel = new VBox(14);
    private final Label pasoUno = paso("1", "Tu consultorio", "Aparece en los informes, presupuestos y recibos.");
    private final Label pasoDos = paso("2", "Cuenta del administrador", "Con ella crearás las cuentas de tu equipo.");

    // Paso 1
    private final TextField nombre = campo("Ej. Clínica Dental Sonrisa Andina");
    private final TextField ruc = campo("11 dígitos (opcional)");
    private final TextField direccion = campo("Ej. Jr. Lima 452, Juliaca");
    private final TextField telefono = campo("Ej. (051) 32-4580");
    private final TextField correo = campo("Ej. citas@miclinica.pe");
    // Paso 2
    private final TextField adminNombre = campo("Ej. Dra. Lucía Paredes Quispe");
    private final TextField adminUsuario = campo("Se sugiere a partir del nombre");
    private final TextField adminCorreo = campo("Opcional");
    private final PasswordField clave = new PasswordField();
    private final PasswordField confirmar = new PasswordField();
    private final CheckBox ejemplos = new CheckBox("Cargar pacientes de ejemplo para conocer DentaVista");
    private boolean usuarioEditado;

    private VisorDental3D visor;

    public Parent vista() {
        visor = new VisorDental3D();
        visor.numerosVisiblesProperty().set(false);
        visor.giroAutomaticoProperty().set(true);
        StackPane.setMargin(visor, new Insets(150, 0, 190, 0));

        Label titulo = new Label("Bienvenido a DentaVista");
        titulo.getStyleClass().add("login-titulo");
        Label lema = new Label("Configura tu consultorio en dos pasos. Solo se hace una vez.");
        lema.getStyleClass().add("login-lema");
        lema.setMaxWidth(460);
        VBox textos = new VBox(12, titulo, lema);
        textos.setPadding(new Insets(54, 40, 0, 60));
        textos.setPickOnBounds(false);
        StackPane.setAlignment(textos, Pos.TOP_LEFT);
        textos.setMaxHeight(Region.USE_PREF_SIZE);

        VBox pasos = new VBox(14, pasoUno, pasoDos);
        pasos.setPadding(new Insets(0, 40, 48, 60));
        pasos.setPickOnBounds(false);
        pasos.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(pasos, Pos.BOTTOM_LEFT);

        StackPane escenario = new StackPane(visor, textos, pasos);
        escenario.getStyleClass().add("login-escenario");
        HBox.setHgrow(escenario, Priority.ALWAYS);

        panel.getStyleClass().addAll("login-panel", "asistente-panel");
        panel.setAlignment(Pos.TOP_LEFT);

        prepararCampos();
        mostrarPasoUno();

        HBox raiz = new HBox(escenario, panel);
        raiz.getStyleClass().add("app");
        return raiz;
    }

    private void prepararCampos() {
        Ui.soloDigitos(ruc, 11);
        Mascaras.correo(correo);
        Mascaras.correo(adminCorreo);
        Mascaras.nombrePropio(adminNombre);
        clave.setPromptText("Mínimo 8 caracteres, letras y números");
        confirmar.setPromptText("Repite la contraseña");
        // El usuario se sugiere solo mientras la persona no lo haya escrito a mano.
        adminNombre.textProperty().addListener((o, a, n) -> {
            if (!usuarioEditado) {
                adminUsuario.setText(usuarios.sugerirUsuario(n));
            }
        });
        adminUsuario.setOnKeyTyped(e -> usuarioEditado = true);
        adminUsuario.setTextFormatter(new javafx.scene.control.TextFormatter<String>(c -> {
            c.setText(c.getText().toLowerCase().replaceAll("[^a-z0-9._-]", ""));
            return c;
        }));
        Ui.tooltip(ejemplos, "Agrega 12 pacientes con historia clínica, citas y pagos, y cuentas de ejemplo "
                + "del personal. Ideal para presentaciones; para un consultorio real déjalo sin marcar.");
    }

    // ------------------------------------------------------------------ pasos

    private void mostrarPasoUno() {
        marcarPaso(1);
        Label error = error();
        GridPane g = rejilla();
        g.add(Ui.campo("Nombre del consultorio *", nombre), 0, 0, 2, 1);
        g.add(Ui.campo("RUC", ruc), 0, 1);
        g.add(Ui.campo("Teléfono", telefono), 1, 1);
        g.add(Ui.campo("Dirección", direccion), 0, 2, 2, 1);
        g.add(Ui.campo("Correo", correo), 0, 3, 2, 1);

        Button seguir = Ui.primario("Continuar", Feather.ARROW_RIGHT);
        seguir.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        seguir.getStyleClass().add("boton-grande");
        seguir.setMaxWidth(Double.MAX_VALUE);
        seguir.setDefaultButton(true);
        seguir.setOnAction(e -> {
            if (nombre.getText() == null || nombre.getText().isBlank()) {
                fallar(error, seguir, "Escribe el nombre del consultorio.");
                nombre.requestFocus();
                return;
            }
            if (!ruc.getText().isBlank() && ruc.getText().length() != 11) {
                fallar(error, seguir, "El RUC debe tener 11 dígitos (o déjalo vacío).");
                return;
            }
            mostrarPasoDos();
        });
        panel.getChildren().setAll(encabezado("Paso 1 de 2", "Datos del consultorio"), g, error,
                Ui.espaciador(), seguir, pie());
        Platform.runLater(nombre::requestFocus);
    }

    private void mostrarPasoDos() {
        marcarPaso(2);
        Label error = error();
        GridPane g = rejilla();
        g.add(Ui.campo("Nombre completo *", adminNombre), 0, 0, 2, 1);
        g.add(Ui.campo("Usuario *", adminUsuario), 0, 1);
        g.add(Ui.campo("Correo", adminCorreo), 1, 1);
        g.add(Ui.campo("Contraseña *", clave), 0, 2);
        g.add(Ui.campo("Confirmar *", confirmar), 1, 2);
        g.add(ClaveSegura.requisitos(clave, confirmar), 0, 3, 2, 1);

        VBox cajaEjemplos = new VBox(4, ejemplos, Ui.etiqueta("Opcional. Útil para presentaciones; puedes registrar "
                + "tus pacientes reales sin marcarlo.", "texto-tenue", "texto-pequeno"));
        ((Label) cajaEjemplos.getChildren().get(1)).setWrapText(true);
        cajaEjemplos.getStyleClass().add("asistente-opcion");

        Button atras = Ui.secundario("Atrás", Feather.ARROW_LEFT);
        atras.setOnAction(e -> mostrarPasoUno());
        Button crear = Ui.primario("Crear consultorio", Feather.CHECK);
        crear.getStyleClass().add("boton-grande");
        crear.setDefaultButton(true);
        HBox.setHgrow(crear, Priority.ALWAYS);
        crear.setMaxWidth(Double.MAX_VALUE);
        HBox botones = new HBox(10, atras, crear);
        botones.setAlignment(Pos.CENTER_LEFT);
        crear.setOnAction(e -> crear(error, crear, botones));

        panel.getChildren().setAll(encabezado("Paso 2 de 2", "Cuenta del administrador"), g, cajaEjemplos, error,
                Ui.espaciador(), botones, pie());
        Platform.runLater(adminNombre::requestFocus);
    }

    private void crear(Label error, Button crear, HBox botones) {
        Consultorio c = Consultorio.builder().nombre(nombre.getText()).ruc(ruc.getText()).direccion(direccion.getText())
                .telefono(telefono.getText()).email(correo.getText()).build();
        var admin = new ConfiguracionInicialService.Administrador(adminNombre.getText(), adminUsuario.getText(),
                adminCorreo.getText(), clave.getText(), confirmar.getText());
        boolean conEjemplos = ejemplos.isSelected();
        String login = adminUsuario.getText();
        String pass = clave.getText();

        // Cargar ejemplos tarda unos segundos: se hace fuera del hilo de la interfaz.
        ProgressIndicator rueda = new ProgressIndicator();
        rueda.setPrefSize(22, 22);
        botones.setDisable(true);
        botones.getChildren().add(rueda);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                configuracion.configurar(c, admin, conEjemplos);
                return null;
            }
        };
        tarea.setOnSucceeded(ev -> {
            visor.giroAutomaticoProperty().set(false);
            usuarios.autenticar(login, pass);
            JavaFxApplication.get().mostrarPanel();
            JavaFxApplication.get().contexto().getBean(com.erejm.dentavista.ui.Notificador.class).exito(
                    "¡Listo! " + c.getNombre() + " está configurado. Crea las cuentas de tu equipo en Usuarios.");
        });
        tarea.setOnFailed(ev -> {
            botones.setDisable(false);
            botones.getChildren().remove(rueda);
            Throwable t = tarea.getException();
            fallar(error, crear, t instanceof NegocioException ? t.getMessage()
                    : "No se pudo configurar: " + t.getMessage());
            if (t.getMessage() != null && (t.getMessage().contains("consultorio") || t.getMessage().contains("RUC"))) {
                mostrarPasoUno();
            }
        });
        Thread hilo = new Thread(tarea, "configuracion-inicial");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ------------------------------------------------------------ utilidades

    private static Node encabezado(String paso, String titulo) {
        Label p = Ui.etiqueta(paso.toUpperCase(), "campo-rotulo");
        Label t = Ui.etiqueta(titulo, "login-bienvenida");
        VBox v = new VBox(4, p, t);
        v.setPadding(new Insets(0, 0, 8, 0));
        return v;
    }

    private static Node pie() {
        return Ui.etiqueta("© 2026 DentaVista · EREJM · Proyecto académico UNAJ", "texto-tenue", "texto-pequeno");
    }

    private static GridPane rejilla() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, c);
        return g;
    }

    private static TextField campo(String ayuda) {
        TextField t = new TextField();
        t.setPromptText(ayuda);
        return t;
    }

    private static Label error() {
        Label l = new Label();
        l.getStyleClass().add("login-error");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private static void fallar(Label error, Button boton, String mensaje) {
        error.setText(mensaje);
        error.setVisible(true);
        error.setManaged(true);
        Animations.shakeX(boton).play();
    }

    private static Label paso(String numero, String titulo, String detalle) {
        Label n = new Label(numero);
        n.getStyleClass().add("asistente-numero");
        n.setMinSize(30, 30);
        n.setAlignment(Pos.CENTER);
        Label t = new Label(titulo + "\n" + detalle);
        t.getStyleClass().add("login-punto");
        t.setGraphic(n);
        t.setGraphicTextGap(14);
        return t;
    }

    private void marcarPaso(int actual) {
        pasoUno.setOpacity(actual == 1 ? 1 : 0.55);
        pasoDos.setOpacity(actual == 2 ? 1 : 0.55);
        ((Label) pasoUno.getGraphic()).getStyleClass().removeAll("activo", "hecho");
        ((Label) pasoDos.getGraphic()).getStyleClass().removeAll("activo", "hecho");
        ((Label) pasoUno.getGraphic()).getStyleClass().add(actual == 1 ? "activo" : "hecho");
        if (actual == 2) {
            ((Label) pasoDos.getGraphic()).getStyleClass().add("activo");
        }
    }
}
