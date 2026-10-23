package com.dentavista.vista;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.modelo.Tratamiento;
import com.dentavista.servicio.CatalogoService;
import com.dentavista.servicio.NegocioException;
import com.dentavista.servicio.SesionService;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Ui;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Catálogo de tratamientos: precios y efecto de cada uno en el odontograma.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class CatalogoVista {

    private final CatalogoService servicio;
    private final SesionService sesion;
    private final Notificador notificador;

    private final TableView<Tratamiento> tabla = new TableView<>();

    public Node vista() {
        Label titulo = Ui.etiqueta("Tratamientos y precios", "titulo-pagina");
        Label sub = Ui.etiqueta("Al marcar un tratamiento como realizado, el odontograma se actualiza con su estado resultante.",
                "subtitulo-pagina");
        Button nuevo = Ui.primario("Nuevo tratamiento", Feather.PLUS);
        nuevo.setOnAction(e -> editar(null));
        nuevo.setVisible(sesion.puedeAdministrar());
        HBox cab = new HBox(12, new VBox(2, titulo, sub), Ui.espaciador(), nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        configurar();
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        if (!sesion.puedeAdministrar()) {
            tarjeta.getChildren().add(0, Ui.chip("Solo el administrador puede modificar precios", "info", Feather.LOCK));
        }
        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        cargar();
        return pagina;
    }

    private void cargar() {
        tabla.getItems().setAll(servicio.todos());
    }

    private void configurar() {
        tabla.getStyleClass().add("tabla");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Tratamiento, String> codigo = col("Código", Tratamiento::getCodigo);
        codigo.setPrefWidth(80);
        TableColumn<Tratamiento, Tratamiento> nombre = col("Tratamiento", t -> t);
        nombre.setPrefWidth(320);
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                VBox v = new VBox(1, Ui.etiqueta(t.getNombre(), "texto-fuerte"));
                if (t.getDescripcion() != null) {
                    v.getChildren().add(Ui.etiqueta(t.getDescripcion(), "texto-tenue", "texto-pequeno"));
                }
                setGraphic(v);
            }
        });
        TableColumn<Tratamiento, Tratamiento> efecto = col("Deja la pieza como", t -> t);
        efecto.setPrefWidth(200);
        efecto.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                EstadoDental e = t.getEstadoResultante();
                Label l = e == null ? Ui.etiqueta("No cambia el odontograma", "texto-tenue")
                        : new Label(e.getEtiqueta(), Ui.muestra(e.getColor()));
                l.setGraphicTextGap(8);
                setGraphic(l);
            }
        });
        TableColumn<Tratamiento, String> alcance = col("Se cobra", t -> !t.isRequierePieza() ? "Por sesión"
                : t.isPorCara() ? "Por cara" : "Por pieza");
        alcance.setPrefWidth(100);
        TableColumn<Tratamiento, String> precio = col("Precio", t -> Formato.soles(t.getPrecio()));
        precio.setPrefWidth(110);
        precio.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Tratamiento, Tratamiento> estado = col("", t -> t);
        estado.setPrefWidth(150);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                if (vacio || t == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(8, t.isActivo() ? Ui.chip("Activo", "exito", null) : Ui.chip("Inactivo", "neutro", null));
                h.setAlignment(Pos.CENTER_RIGHT);
                if (sesion.puedeAdministrar()) {
                    Button ed = Ui.botonIcono(Feather.EDIT_2, "Editar");
                    ed.setOnAction(e -> editar(t));
                    h.getChildren().add(ed);
                }
                setGraphic(h);
            }
        });
        tabla.getColumns().setAll(List.of(codigo, nombre, efecto, alcance, precio, estado));
    }

    private void editar(Tratamiento existente) {
        Tratamiento t = existente != null ? existente : Tratamiento.builder().requierePieza(true).activo(true).build();
        TextField codigo = new TextField(t.getCodigo());
        TextField nombre = new TextField(t.getNombre());
        TextField desc = new TextField(t.getDescripcion());
        TextField precio = new TextField(t.getPrecio() == null ? "" : t.getPrecio().toPlainString());
        com.dentavista.ui.Mascaras.dinero(precio);
        ComboBox<EstadoDental> resultado = new ComboBox<>();
        resultado.getItems().add(null);
        for (EstadoDental e : EstadoDental.values()) {
            if (!e.esPatologia() && e != EstadoDental.SANO) {
                resultado.getItems().add(e);
            }
        }
        resultado.setValue(t.getEstadoResultante());
        resultado.setMaxWidth(Double.MAX_VALUE);
        resultado.setConverter(new StringConverter<>() {
            @Override
            public String toString(EstadoDental e) {
                return e == null ? "No cambia el odontograma" : e.getEtiqueta();
            }

            @Override
            public EstadoDental fromString(String s) {
                return null;
            }
        });
        CheckBox porCara = new CheckBox("Se aplica y cobra por cara");
        porCara.setSelected(t.isPorCara());
        CheckBox requiere = new CheckBox("Se realiza sobre una pieza");
        requiere.setSelected(t.isRequierePieza());
        CheckBox activo = new CheckBox("Activo (disponible para nuevos planes)");
        activo.setSelected(t.isActivo());

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(Ui.campo("Código", codigo), 0, 0);
        g.add(Ui.campo("Precio (S/)", precio), 1, 0);
        g.add(Ui.campo("Nombre", nombre), 0, 1, 2, 1);
        g.add(Ui.campo("Descripción", desc), 0, 2, 2, 1);
        g.add(Ui.campo("Estado resultante en el odontograma", resultado), 0, 3, 2, 1);
        g.add(new VBox(8, requiere, porCara, activo), 0, 4, 2, 1);

        notificador.dialogo(Feather.LAYERS, existente == null ? "Nuevo tratamiento" : "Editar tratamiento", g,
                "Guardar", false, () -> {
                    t.setCodigo(codigo.getText());
                    t.setNombre(nombre.getText());
                    t.setDescripcion(desc.getText() == null || desc.getText().isBlank() ? null : desc.getText().trim());
                    try {
                        t.setPrecio(new BigDecimal(precio.getText().trim().replace(",", ".")));
                    } catch (NumberFormatException ex) {
                        throw new NegocioException("El precio debe ser un número, por ejemplo 120.50");
                    }
                    t.setEstadoResultante(resultado.getValue());
                    t.setPorCara(porCara.isSelected());
                    t.setRequierePieza(requiere.isSelected() || porCara.isSelected());
                    t.setActivo(activo.isSelected());
                    servicio.guardar(t);
                    notificador.exito("Tratamiento \"" + t.getNombre() + "\" guardado.");
                    cargar();
                }, 560);
    }

    private static <T> TableColumn<Tratamiento, T> col(String titulo, Function<Tratamiento, T> valor) {
        TableColumn<Tratamiento, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
