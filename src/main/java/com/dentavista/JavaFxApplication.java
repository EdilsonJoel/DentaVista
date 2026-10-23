package com.dentavista;

import com.dentavista.tresd.ModeloDental;
import com.dentavista.ui.Tema;
import com.dentavista.vista.LoginController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

/**
 * Arranca la aplicación: muestra al instante una pantalla de carga y, en segundo
 * plano, levanta Spring (base de datos y servicios) y prepara el modelo 3D. Cuando
 * todo está listo pasa a la pantalla de acceso. Así la app nunca parece "colgada".
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public class JavaFxApplication extends Application {

    private static JavaFxApplication instancia;
    private ConfigurableApplicationContext context;
    private Stage ventana;

    public JavaFxApplication() {
        instancia = this;
    }

    public static JavaFxApplication get() {
        return instancia;
    }

    /** Levanta Spring y el modelo 3D (se ejecuta fuera del hilo de la interfaz). */
    private void cargarServicios() {
        Thread precarga = new Thread(ModeloDental::get, "precarga-modelo-3d");
        precarga.setDaemon(true);
        precarga.start();
        java.util.List<String> args = new java.util.ArrayList<>(getParameters().getRaw());
        if (args.stream().noneMatch(a -> a.startsWith("--spring.datasource.url"))
                && System.getProperty("spring.datasource.url") == null) {
            args.add("--spring.datasource.url=jdbc:sqlite:" + baseDeDatos());
        }
        context = new SpringApplicationBuilder(DentaVistaApplication.class)
                .web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
        try {
            precarga.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * La base va en la carpeta del usuario (%LOCALAPPDATA%\DentaVista), no junto al .exe:
     * si la app se instala en Program Files esa carpeta no es escribible, y así cada
     * usuario de la PC tiene sus propios datos.
     */
    private static java.nio.file.Path baseDeDatos() {
        String local = System.getenv("LOCALAPPDATA");
        java.nio.file.Path carpeta = local != null && !local.isBlank()
                ? java.nio.file.Path.of(local, "DentaVista")
                : java.nio.file.Path.of(System.getProperty("user.home"), ".dentavista");
        try {
            java.nio.file.Files.createDirectories(carpeta);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("No se pudo crear la carpeta de datos " + carpeta, e);
        }
        return carpeta.resolve("dentavista.db");
    }

    @Override
    public void start(Stage stage) {
        this.ventana = stage;
        try (var icono = getClass().getResourceAsStream("/img/logo.png")) {
            if (icono != null) {
                stage.getIcons().add(new Image(icono));
            }
        } catch (Exception ignorado) {
            // sin icono no pasa nada
        }
        stage.setTitle("DentaVista · Gestión clínica dental");
        stage.setScene(pantallaDeCarga());
        stage.centerOnScreen();
        stage.show();

        Thread arranque = new Thread(() -> {
            try {
                cargarServicios();
                Platform.runLater(() -> {
                    context.getBean(Tema.class).aplicarInicial();
                    if (context.getBean(com.dentavista.servicio.ConfiguracionInicialService.class).requiereConfiguracion()) {
                        mostrarConfiguracion();
                    } else {
                        mostrarLogin();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                            "No se pudo iniciar DentaVista:\n" + e.getMessage());
                    a.setHeaderText("Error al iniciar");
                    a.showAndWait();
                    Platform.exit();
                });
            }
        }, "arranque-dentavista");
        arranque.setDaemon(true);
        arranque.start();
    }

    /** Pantalla de carga: logo, nombre y una barra animada. No depende de Spring. */
    private Scene pantallaDeCarga() {
        javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView();
        try (var in = getClass().getResourceAsStream("/img/logo.png")) {
            if (in != null) {
                logo.setImage(new Image(in));
            }
        } catch (Exception ignorado) {
            // sin logo
        }
        logo.setFitWidth(84);
        logo.setFitHeight(84);
        javafx.scene.control.Label nombre = new javafx.scene.control.Label("DentaVista");
        nombre.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");
        javafx.scene.control.Label lema = new javafx.scene.control.Label("Gestión clínica dental");
        lema.setStyle("-fx-font-size: 14px; -fx-text-fill: #9fb0c8;");
        javafx.scene.control.ProgressBar barra = new javafx.scene.control.ProgressBar(-1);
        barra.setPrefWidth(220);
        barra.setStyle("-fx-accent: #14b8a6;");
        javafx.scene.control.Label estado = new javafx.scene.control.Label("Preparando el consultorio…");
        estado.setStyle("-fx-font-size: 12px; -fx-text-fill: #7c8aa3;");
        javafx.scene.layout.VBox caja = new javafx.scene.layout.VBox(12, logo, nombre, lema, barra, estado);
        caja.setAlignment(javafx.geometry.Pos.CENTER);
        caja.setStyle("-fx-background-color: radial-gradient(center 50% 40%, radius 80%, #16263f 0%, #0a101c 75%);");
        return new Scene(caja, 520, 360);
    }

    /** Contexto de Spring (para pruebas de interfaz automatizadas). */
    public ConfigurableApplicationContext contexto() {
        return context;
    }

    public HostServices hostServices() {
        return getHostServices();
    }

    public Stage ventana() {
        return ventana;
    }

    public void mostrarLogin() {
        Parent raiz = cargar("/view/login-view.fxml");
        Scene escena = escena(raiz, 1180, 740);
        ventana.setScene(escena);
        ventana.setMinWidth(1000);
        ventana.setMinHeight(660);
        ventana.centerOnScreen();
        context.getBean(LoginController.class).alMostrar();
    }

    /** Primer uso: asistente para registrar el consultorio y su administrador. */
    public void mostrarConfiguracion() {
        Parent raiz = context.getBean(com.dentavista.vista.ConfiguracionInicialVista.class).vista();
        Scene escena = escena(raiz, 1240, 780);
        ventana.setScene(escena);
        ventana.setMinWidth(1080);
        ventana.setMinHeight(700);
        ventana.centerOnScreen();
    }

    public void mostrarPanel() {
        Parent raiz = cargar("/view/main-view.fxml");
        Scene escena = escena(raiz, 1440, 900);
        ventana.setScene(escena);
        ventana.setMinWidth(1180);
        ventana.setMinHeight(720);
        ventana.setMaximized(true);
    }

    private Scene escena(Parent raiz, double w, double h) {
        Scene s = new Scene(raiz, w, h);
        s.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        context.getBean(Tema.class).registrar(raiz);
        return s;
    }

    private Parent cargar(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);
            return loader.load();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar " + fxml, e);
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
        Platform.exit();
    }
}
