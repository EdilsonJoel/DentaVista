package com.erejm.dentavista.vista;

import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.servicio.PacienteService;
import com.erejm.dentavista.ui.Formato;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Ui;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lista de pacientes con búsqueda instantánea por nombre o DNI y tres filtros:
 * activos, para dar de alta (terminaron y pagaron todo) y archivados.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class PacientesVista {

    private final PacienteService servicio;
    private final Notificador notificador;
    private final Navegador navegador;

    /** Qué grupo de pacientes se lista. */
    enum Filtro { ACTIVOS, ALTA, ARCHIVADOS }

    private final TableView<Paciente> tabla = new TableView<>();
    private Filtro filtro = Filtro.ACTIVOS;
    private String texto = "";
    private final javafx.scene.control.ToggleButton botonAlta = new javafx.scene.control.ToggleButton();
    private final Map<Long, LocalDateTime> ultimas = new HashMap<>();
    private final Label contador = new Label();

    public Node vista() {
        Label titulo = Ui.etiqueta("Pacientes", "titulo-pagina");
        Label sub = Ui.etiqueta("Busca un paciente y abre su odontograma 3D.", "subtitulo-pagina");
        Button nuevo = Ui.primario("Nuevo paciente", Feather.USER_PLUS);
        nuevo.setOnAction(e -> FormularioPaciente.abrir(notificador, servicio, null, navegador::abrirFicha));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), Ui.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        TextField buscar = new TextField();
        buscar.setPromptText("Buscar por nombre, apellido o DNI…");
        buscar.getStyleClass().add("buscador");
        HBox.setHgrow(buscar, Priority.ALWAYS);
        buscar.textProperty().addListener((o, a, t) -> {
            texto = t;
            cargar();
        });
        contador.getStyleClass().addAll("texto-tenue");
        HBox barra = new HBox(12, Ui.icono(Feather.SEARCH, 16), buscar, contador, filtros());
        barra.setAlignment(Pos.CENTER_LEFT);

        configurarTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(barra, tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);

        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        cargar();
        javafx.application.Platform.runLater(buscar::requestFocus);
        return pagina;
    }

    private Node filtros() {
        javafx.scene.control.ToggleGroup g = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.ToggleButton activos = new javafx.scene.control.ToggleButton("Activos");
        botonAlta.setGraphic(Ui.icono(Feather.CHECK_CIRCLE, 14));
        Ui.tooltip(botonAlta, "Terminaron su plan, lo pagaron y no tienen citas: puedes archivarlos");
        javafx.scene.control.ToggleButton archivados = new javafx.scene.control.ToggleButton("Archivados", Ui.icono(Feather.ARCHIVE, 14));
        activos.setUserData(Filtro.ACTIVOS);
        botonAlta.setUserData(Filtro.ALTA);
        archivados.setUserData(Filtro.ARCHIVADOS);
        activos.getStyleClass().add("left-pill");
        botonAlta.getStyleClass().add("center-pill");
        archivados.getStyleClass().add("right-pill");
        for (var t : List.of(activos, botonAlta, archivados)) {
            t.setToggleGroup(g);
            t.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        activos.setSelected(true);
        g.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            filtro = (Filtro) b.getUserData();
            cargar();
        });
        return new HBox(activos, botonAlta, archivados);
    }

    private void cargar() {
        List<Paciente> alta = servicio.paraDarDeAlta();
        botonAlta.setText("Para dar de alta (" + alta.size() + ")");
        String q = texto == null ? "" : texto.trim().toLowerCase();
        List<Paciente> lista = switch (filtro) {
            case ACTIVOS -> servicio.buscar(texto);
            case ARCHIVADOS -> servicio.archivados(texto);
            case ALTA -> alta.stream().filter(p -> q.isEmpty() || p.nombreCompleto().toLowerCase().contains(q)
                    || p.getDni().contains(q)).toList();
        };
        lista.forEach(p -> ultimas.computeIfAbsent(p.getId(), id -> servicio.ultimaVisita(p).orElse(null)));
        tabla.getItems().setAll(lista);
        contador.setText(lista.size() + (lista.size() == 1 ? " paciente" : " pacientes"));
        tabla.setPlaceholder(switch (filtro) {
            case ACTIVOS -> Ui.vacio(Feather.USERS, "Sin resultados", "Prueba con otro nombre o registra un paciente nuevo.");
            case ALTA -> Ui.vacio(Feather.CHECK_CIRCLE, "Nadie para dar de alta",
                    "Aquí aparecen quienes terminaron y pagaron todo su plan y no tienen citas por venir.");
            case ARCHIVADOS -> Ui.vacio(Feather.ARCHIVE, "No hay pacientes archivados",
                    "Al archivar a un paciente su historia clínica se conserva y puedes reactivarlo cuando vuelva.");
        });
    }

    private void archivar(Paciente p) {
        Label info = Ui.etiqueta(p.nombreCompleto() + " dejará de aparecer en la lista, la agenda y los avisos. "
                + "Su historia clínica, odontograma y pagos se conservan, y puedes reactivarlo cuando vuelva. "
                + "Sus citas por venir se cancelan y su acceso queda desactivado.", "texto-tenue");
        info.setWrapText(true);
        notificador.dialogo(Feather.ARCHIVE, "Archivar paciente", new VBox(info), "Archivar", true, () -> {
            servicio.archivar(p);
            notificador.exito(p.nombreCompleto() + " fue archivado.");
            cargar();
        }, 480);
    }

    private void reactivar(Paciente p) {
        if (notificador.intentar(() -> servicio.reactivar(p))) {
            notificador.exito(p.nombreCompleto() + " vuelve a estar activo.");
            cargar();
        }
    }

    private void configurarTabla() {
        tabla.getStyleClass().addAll("tabla", "striped");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Paciente, Paciente> nombre = columna("Paciente", p -> p);
        nombre.setPrefWidth(300);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Label n = Ui.etiqueta(p.nombreCompleto(), "texto-fuerte");
                Label d = Ui.etiqueta("DNI " + p.getDni(), "texto-tenue", "texto-pequeno");
                HBox h = new HBox(10, Ui.avatar(p.iniciales(), p.getDni(), 34), new VBox(1, n, d));
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });

        TableColumn<Paciente, String> edad = columna("Edad", p -> p.edad() == null ? "—" : p.edad() + " años");
        edad.setPrefWidth(90);
        TableColumn<Paciente, String> tel = columna("Teléfono", p -> p.getTelefono() == null ? "—" : p.getTelefono());
        tel.setPrefWidth(130);

        TableColumn<Paciente, Paciente> alertas = columna("Alertas médicas", p -> p);
        alertas.setPrefWidth(260);
        alertas.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_LEFT);
                if (p.getAlergias() != null) {
                    h.getChildren().add(Ui.chip("Alergia: " + p.getAlergias(), "peligro", Feather.ALERT_OCTAGON));
                }
                if (p.getAntecedentes() != null) {
                    String a = p.getAntecedentes();
                    h.getChildren().add(Ui.chip(a.length() > 26 ? a.substring(0, 25) + "…" : a, "alerta", Feather.HEART));
                }
                if (h.getChildren().isEmpty()) {
                    h.getChildren().add(Ui.etiqueta("—", "texto-tenue"));
                }
                setGraphic(h);
            }
        });

        TableColumn<Paciente, String> visita = columna("Última visita", p -> Formato.relativo(ultimas.get(p.getId())));
        visita.setPrefWidth(130);

        TableColumn<Paciente, Paciente> acciones = columna("", p -> p);
        acciones.setPrefWidth(200);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Paciente p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setGraphic(null);
                    return;
                }
                Button abrir = Ui.secundario("Abrir ficha", Feather.ARROW_RIGHT);
                abrir.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
                abrir.setOnAction(e -> navegador.abrirFicha(p));
                Button extra;
                if (p.isActivo()) {
                    extra = Ui.botonIcono(Feather.ARCHIVE, "Archivar (dar de baja)");
                    extra.setOnAction(e -> archivar(p));
                } else {
                    extra = Ui.botonIcono(Feather.ROTATE_CCW, "Reactivar paciente");
                    extra.setOnAction(e -> reactivar(p));
                }
                HBox h = new HBox(6, extra, abrir);
                h.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(h);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        tabla.getColumns().setAll(List.of(nombre, edad, tel, alertas, visita, acciones));
        tabla.setRowFactory(t -> {
            TableRow<Paciente> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    navegador.abrirFicha(fila.getItem());
                }
            });
            return fila;
        });
    }

    private static <T> TableColumn<Paciente, T> columna(String titulo, Function<Paciente, T> valor) {
        TableColumn<Paciente, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
