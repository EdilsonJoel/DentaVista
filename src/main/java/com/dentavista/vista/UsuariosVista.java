package com.dentavista.vista;

import com.dentavista.modelo.Consultorio;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import com.dentavista.servicio.DatosClinica;
import com.dentavista.servicio.Seguridad;
import com.dentavista.servicio.UsuarioService;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Mascaras;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Ui;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cuentas del equipo del consultorio (solo administrador).
 * <p>
 * Crear una cuenta toma pocos segundos: se elige el rol, se escribe el nombre (el usuario
 * se sugiere solo) y DentaVista genera una contraseña temporal para entregarla. En su
 * primer ingreso la persona crea la suya, así nadie más la conoce. Desde la lista se
 * restablece la contraseña o se desactiva una cuenta sin perder su historial.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class UsuariosVista {

    /** Qué puede hacer cada rol (se muestra al elegirlo). */
    private static final Map<Rol, String> PERMISOS = Map.of(
            Rol.ODONTOLOGO, "Hallazgos, planes, recetas e informes. Atiende las citas.",
            Rol.ASISTENTE, "Agenda, pacientes y pagos. Ve la historia clínica sin modificarla.",
            Rol.ADMIN, "Todo, más las cuentas del equipo, los precios y el consultorio.");

    private final UsuarioService servicio;
    private final DatosClinica clinica;
    private final Notificador notificador;

    private final TableView<Usuario> tabla = new TableView<>();
    private final Label resumen = new Label();

    public Node vista() {
        Label titulo = Ui.etiqueta("Usuarios", "titulo-pagina");
        Label sub = Ui.etiqueta("Cada persona del equipo entra con su propia cuenta y solo ve lo que su rol permite.",
                "subtitulo-pagina");
        Button consultorio = Ui.secundario("Datos del consultorio", Feather.HOME);
        consultorio.setOnAction(e -> editarConsultorio());
        Button nuevo = Ui.primario("Nueva cuenta", Feather.USER_PLUS);
        nuevo.setOnAction(e -> editar(null));
        HBox cab = new HBox(10, new VBox(2, titulo, sub), Ui.espaciador(), consultorio, nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        configurarTabla();
        resumen.getStyleClass().add("texto-tenue");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(resumen, tabla);
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        VBox pagina = new VBox(18, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        cargar();
        return pagina;
    }

    private void cargar() {
        List<Usuario> lista = servicio.listar();
        tabla.getItems().setAll(lista);
        long activos = lista.stream().filter(Usuario::isActivo).count();
        resumen.setText(cuenta(lista, Rol.ODONTOLOGO, "odontólogo", "odontólogos") + "  ·  "
                + cuenta(lista, Rol.ASISTENTE, "asistente", "asistentes") + "  ·  "
                + cuenta(lista, Rol.ADMIN, "administrador", "administradores") + "  ·  "
                + activos + (activos == 1 ? " cuenta activa" : " cuentas activas"));
    }

    private static String cuenta(List<Usuario> lista, Rol rol, String uno, String varios) {
        long n = lista.stream().filter(u -> u.getRol() == rol).count();
        return n + " " + (n == 1 ? uno : varios);
    }

    // ------------------------------------------------------------------ tabla

    private void configurarTabla() {
        tabla.getStyleClass().addAll("tabla", "striped");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(Ui.vacio(Feather.USERS, "Aún no hay cuentas", "Crea la cuenta de cada persona del equipo."));

        TableColumn<Usuario, Usuario> persona = col("Persona", u -> u);
        persona.setPrefWidth(320);
        persona.setCellFactory(c -> celda(u -> {
            Label n = Ui.etiqueta(u.getNombre(), "texto-fuerte");
            Label d = Ui.etiqueta("@" + u.getUsuario() + (u.getEmail() != null ? "  ·  " + u.getEmail() : ""),
                    "texto-tenue", "texto-pequeno");
            HBox h = new HBox(10, Ui.avatar(u.iniciales(), u.getNombre(), 34), new VBox(1, n, d));
            h.setAlignment(Pos.CENTER_LEFT);
            return h;
        }));

        TableColumn<Usuario, Usuario> rol = col("Rol", u -> u);
        rol.setPrefWidth(150);
        rol.setCellFactory(c -> celda(u -> Ui.chip(u.getRol().getEtiqueta(), switch (u.getRol()) {
            case ADMIN -> "alerta";
            case ODONTOLOGO -> "info";
            default -> "neutro";
        }, switch (u.getRol()) {
            case ADMIN -> Feather.SHIELD;
            case ODONTOLOGO -> Feather.ACTIVITY;
            default -> Feather.CALENDAR;
        })));

        TableColumn<Usuario, String> cop = col("Colegiatura", u -> u.getColegiatura() == null ? "—" : u.getColegiatura());
        cop.setPrefWidth(120);

        TableColumn<Usuario, Usuario> acceso = col("Último acceso", u -> u);
        acceso.setPrefWidth(170);
        acceso.setCellFactory(c -> celda(u -> u.requiereCambioClave()
                ? Ui.chip("Contraseña temporal", "alerta", Feather.CLOCK)
                : Ui.etiqueta(u.getUltimoAcceso() == null ? "Nunca" : Formato.relativo(u.getUltimoAcceso()), "texto-tenue")));

        TableColumn<Usuario, Usuario> acciones = col("", u -> u);
        acciones.setPrefWidth(200);
        acciones.setCellFactory(c -> celda(u -> {
            Label estado = u.isActivo() ? Ui.chip("Activa", "exito", null) : Ui.chip("Desactivada", "neutro", null);
            Button ed = Ui.botonIcono(Feather.EDIT_2, "Editar cuenta");
            ed.setOnAction(e -> editar(u));
            Button clave = Ui.botonIcono(Feather.KEY, "Restablecer contraseña");
            clave.setOnAction(e -> restablecer(u));
            HBox h = new HBox(6, estado, Ui.espaciador(), clave, ed);
            h.setAlignment(Pos.CENTER_RIGHT);
            return h;
        }));

        tabla.getColumns().setAll(List.of(persona, rol, cop, acceso, acciones));
        tabla.setRowFactory(t -> {
            var fila = new javafx.scene.control.TableRow<Usuario>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    editar(fila.getItem());
                }
            });
            return fila;
        });
    }

    // ------------------------------------------------------- crear / editar

    private void editar(Usuario existente) {
        boolean nueva = existente == null;
        Usuario u = nueva ? Usuario.builder().rol(Rol.ODONTOLOGO).activo(true).build() : existente;

        // Rol: tarjetas con lo que permite cada uno.
        ToggleGroup roles = new ToggleGroup();
        HBox tarjetasRol = new HBox(8);
        tarjetasRol.setPrefHeight(96);
        tarjetasRol.setMaxHeight(96);
        for (Rol r : List.of(Rol.ODONTOLOGO, Rol.ASISTENTE, Rol.ADMIN)) {
            Label t = new Label(r.getEtiqueta(), Ui.icono(switch (r) {
                case ADMIN -> Feather.SHIELD;
                case ODONTOLOGO -> Feather.ACTIVITY;
                default -> Feather.CALENDAR;
            }, 15));
            t.getStyleClass().add("texto-fuerte");
            t.setGraphicTextGap(6);
            Label d = Ui.etiqueta(PERMISOS.get(r), "texto-tenue", "texto-pequeno");
            d.setWrapText(true);
            d.setPrefWidth(158);
            d.setMinHeight(Region.USE_PREF_SIZE);
            VBox contenidoRol = new VBox(4, t, d);
            ToggleButton b = new ToggleButton();
            b.setGraphic(contenidoRol);
            b.getStyleClass().add("rol-opcion");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMaxHeight(Double.MAX_VALUE);
            b.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setUserData(r);
            b.setToggleGroup(roles);
            b.setSelected(r == u.getRol());
            tarjetasRol.getChildren().add(b);
        }
        roles.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
            }
        });

        TextField nombre = new TextField(u.getNombre());
        nombre.setPromptText("Ej. Dra. Lucía Paredes Quispe");
        Mascaras.nombrePropio(nombre);
        TextField login = new TextField(u.getUsuario());
        login.setPromptText("Se sugiere a partir del nombre");
        login.setTextFormatter(new TextFormatter<String>(c -> {
            c.setText(c.getText().toLowerCase().replaceAll("[^a-z0-9._-]", ""));
            return c;
        }));
        boolean[] editado = {!nueva};
        login.setOnKeyTyped(e -> editado[0] = true);
        nombre.textProperty().addListener((o, a, n) -> {
            if (!editado[0]) {
                login.setText(servicio.sugerirUsuario(n));
            }
        });
        TextField email = new TextField(u.getEmail());
        email.setPromptText("Opcional");
        Mascaras.correo(email);
        TextField cop = new TextField(u.getColegiatura());
        cop.setPromptText("COP 12345");
        VBox campoCop = Ui.campo("Colegiatura (COP) *", cop);
        Runnable mostrarCop = () -> {
            boolean odontologo = roles.getSelectedToggle() != null && roles.getSelectedToggle().getUserData() == Rol.ODONTOLOGO;
            campoCop.setVisible(odontologo);
            campoCop.setManaged(odontologo);
        };
        roles.selectedToggleProperty().addListener((o, a, b) -> mostrarCop.run());
        mostrarCop.run();

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
        col.setPercentWidth(50);
        g.getColumnConstraints().addAll(col, col);
        g.add(Ui.campo("Nombre completo *", nombre), 0, 0, 2, 1);
        g.add(Ui.campo("Usuario *", login), 0, 1);
        g.add(Ui.campo("Correo", email), 1, 1);
        g.add(campoCop, 0, 2);

        VBox contenido = new VBox(14, Ui.campo("Rol", tarjetasRol), g);
        CheckBox activa = new CheckBox("Cuenta activa (desactívala si la persona deja el consultorio; su historial se conserva)");
        activa.setWrapText(true);
        activa.setSelected(u.isActivo());
        if (nueva) {
            Label aviso = Ui.etiqueta("Al crearla, DentaVista genera una contraseña temporal para que se la entregues. "
                    + "En su primer ingreso la persona creará la suya.", "texto-tenue", "texto-pequeno");
            aviso.setWrapText(true);
            aviso.setMinHeight(Region.USE_PREF_SIZE);
            HBox.setHgrow(aviso, Priority.ALWAYS);
            aviso.setMaxWidth(Double.MAX_VALUE);
            HBox caja = new HBox(10, Ui.icono(Feather.LOCK, 16), aviso);
            caja.setAlignment(Pos.CENTER_LEFT);
            caja.getStyleClass().add("asistente-opcion");
            contenido.getChildren().add(caja);
        } else {
            contenido.getChildren().add(activa);
        }

        notificador.dialogo(nueva ? Feather.USER_PLUS : Feather.EDIT_2, nueva ? "Nueva cuenta" : "Editar cuenta",
                contenido, nueva ? "Crear cuenta" : "Guardar", false, () -> {
                    u.setRol((Rol) roles.getSelectedToggle().getUserData());
                    u.setNombre(nombre.getText());
                    u.setUsuario(login.getText());
                    u.setEmail(email.getText());
                    u.setColegiatura(u.getRol() == Rol.ODONTOLOGO ? cop.getText() : null);
                    if (nueva) {
                        String temporal = Seguridad.claveTemporal();
                        Usuario creada = servicio.guardar(u, temporal, true);
                        cargar();
                        com.dentavista.ui.Credenciales.mostrarDespues(notificador, "Cuenta creada", creada.getNombre(), creada.getUsuario(), temporal, clinica.nombre());
                    } else {
                        u.setActivo(activa.isSelected());
                        servicio.guardar(u, null);
                        notificador.exito("Cuenta de " + u.getNombre() + " actualizada.");
                        cargar();
                    }
                }, 600);
    }

    private void restablecer(Usuario u) {
        notificador.confirmar("Restablecer contraseña",
                "Se generará una contraseña temporal para " + u.getNombre() + ". La actual dejará de funcionar y, al "
                        + "ingresar, deberá crear una nueva.", "Restablecer", false, () -> {
                    String temporal = servicio.restablecerClave(u);
                    cargar();
                    com.dentavista.ui.Credenciales.mostrarDespues(notificador, "Contraseña restablecida", u.getNombre(), u.getUsuario(), temporal, clinica.nombre());
                });
    }

    // ------------------------------------------------------------ consultorio

    private void editarConsultorio() {
        Consultorio c = clinica.actual() == null ? new Consultorio() : clinica.actual();
        TextField nombre = new TextField(c.getNombre());
        TextField ruc = new TextField(c.getRuc());
        Ui.soloDigitos(ruc, 11);
        TextField direccion = new TextField(c.getDireccion());
        TextField telefono = new TextField(c.getTelefono());
        TextField correo = new TextField(c.getEmail());
        Mascaras.correo(correo);
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
        col.setPercentWidth(50);
        g.getColumnConstraints().addAll(col, col);
        g.add(Ui.campo("Nombre del consultorio *", nombre), 0, 0, 2, 1);
        g.add(Ui.campo("RUC", ruc), 0, 1);
        g.add(Ui.campo("Teléfono", telefono), 1, 1);
        g.add(Ui.campo("Dirección", direccion), 0, 2, 2, 1);
        g.add(Ui.campo("Correo", correo), 0, 3, 2, 1);
        Label nota = Ui.etiqueta("Aparecen en la cabecera de informes, presupuestos, recibos y recetas.",
                "texto-tenue", "texto-pequeno");
        notificador.dialogo(Feather.HOME, "Datos del consultorio", new VBox(12, g, nota), "Guardar", false, () -> {
            clinica.guardar(Consultorio.builder().nombre(nombre.getText()).ruc(ruc.getText())
                    .direccion(direccion.getText()).telefono(telefono.getText()).email(correo.getText()).build());
            notificador.exito("Datos del consultorio guardados.");
        }, 560);
    }

    // ------------------------------------------------------------- utilidades

    private static TableCell<Usuario, Usuario> celda(Function<Usuario, Node> grafico) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Usuario u, boolean vacio) {
                super.updateItem(u, vacio);
                setGraphic(vacio || u == null ? null : grafico.apply(u));
            }
        };
    }

    private static <T> TableColumn<Usuario, T> col(String titulo, Function<Usuario, T> valor) {
        TableColumn<Usuario, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
