package com.dentavista.vista;

import com.dentavista.JavaFxApplication;
import com.dentavista.dominio.EstadoDental;
import com.dentavista.dominio.Odontograma;
import com.dentavista.dominio.Pieza;
import com.dentavista.dominio.Superficie;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.Hallazgo;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.MedioPago;
import com.dentavista.modelo.Pago;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Tratamiento;
import com.dentavista.servicio.CatalogoService;
import com.dentavista.servicio.ClinicaService;
import com.dentavista.servicio.NegocioException;
import com.dentavista.servicio.PacienteService;
import com.dentavista.servicio.PagoService;
import com.dentavista.servicio.PdfService;
import com.dentavista.servicio.PlanService;
import com.dentavista.servicio.SesionService;
import com.dentavista.tresd.VisorDental3D;
import com.dentavista.ui.Capturas;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Imagenes;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Odontograma2D;
import com.dentavista.ui.Ui;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Ficha del paciente: el corazón de DentaVista.
 * <ul>
 *   <li><b>Odontograma</b>: visor 3D + odontograma 2D sincronizados, paleta de
 *       hallazgos, panel de la pieza seleccionada y línea de tiempo.</li>
 *   <li><b>Plan y presupuesto</b>: tratamientos generados desde el odontograma,
 *       realizados / pendientes y PDF.</li>
 *   <li><b>Historial</b>: cada consulta con sus hallazgos y tratamientos.</li>
 * </ul>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class FichaVista {

    private final ClinicaService clinica;
    private final PlanService planes;
    private final PacienteService pacientes;
    private final CatalogoService catalogo;
    private final SesionService sesion;
    private final PdfService pdf;
    private final Notificador notificador;
    private final Navegador navegador;
    private final com.dentavista.servicio.CitaService citas;
    private final com.dentavista.servicio.UsuarioService usuarios;
    private final DialogoCita dialogoCita;
    private final com.dentavista.servicio.PagoService pagos;
    private final com.dentavista.servicio.RecetaService recetas;
    private final DialogoReceta dialogoReceta;
    private final DialogoHistorialPieza dialogoHistorialPieza;
    private final com.dentavista.servicio.DatosClinica datosClinica;

    private Paciente paciente;
    private Odontograma actual = Odontograma.vacio();
    private Integer piezaSel;
    private Superficie caraSel;
    /** Hallazgo que se pinta con cada clic; null = solo seleccionar. */
    private EstadoDental herramienta;
    /** Fecha que se está viendo en la línea de tiempo; null = hoy. */
    private LocalDateTime fechaVista;
    private List<Consulta> cronologia = new ArrayList<>();

    private final BorderPane raiz = new BorderPane();
    private final StackPane cuerpo = new StackPane();
    private final HBox cabecera = new HBox();
    private VisorDental3D visor;
    private Odontograma2D o2d;
    private final VBox panelPieza = new VBox();
    private final Label estadoRegistro = new Label();
    private final ToggleGroup grupoHerramientas = new ToggleGroup();
    private final Map<EstadoDental, ToggleButton> botonesHerramienta = new LinkedHashMap<>();
    private ToggleButton botonSeleccionar;
    private final Slider linea = new Slider();
    private final Label lblFecha = new Label();
    private final javafx.scene.layout.Pane marcas = new javafx.scene.layout.Pane();
    private final Label avisoHistorico = new Label();
    private Timeline reproduccion;
    private boolean ajustandoLinea;
    private final TextArea nota = new TextArea();
    /** Escenario 3D (visor + barra + línea de tiempo); se traslada a una ventana propia en pantalla completa. */
    private StackPane escenario;
    private final ToggleButton botonPantallaCompleta = new ToggleButton(null, Ui.icono(Feather.MAXIMIZE, 15));
    private javafx.stage.Stage ventanaCompleta;
    private javafx.event.EventHandler<KeyEvent> atajos;

    private Node pestanaOdontograma;
    private Node pestanaPlan;
    private Node pestanaHistorial;
    private final ToggleGroup grupoPestanas = new ToggleGroup();
    private ToggleButton tabOdontograma;

    private final TableView<ItemPlan> tablaPlan = new TableView<>();
    private final VBox totalesPlan = new VBox();
    private final VBox panelCuenta = new VBox(14);
    private List<com.dentavista.modelo.Receta> recetasDelPaciente = List.of();
    private final VBox listaHistorial = new VBox(0);
    /** Pieza por la que se filtra la pestaña Historial (null = todas). */
    private Integer filtroPieza;

    public Node vista(Paciente p) {
        this.paciente = pacientes.obtener(p.getId());

        construirCabecera();
        pestanaOdontograma = construirOdontograma();
        pestanaPlan = construirPlan();
        pestanaHistorial = construirHistorial();
        cuerpo.getChildren().setAll(pestanaOdontograma);

        raiz.setTop(cabecera);
        raiz.setCenter(cuerpo);
        if (!sesion.esPaciente()) {
            instalarAtajos();
        }

        recargarTodo();
        seleccionar(primeraPiezaConHallazgo(), null);
        return raiz;
    }

    // =================================================================== cabecera

    private void construirCabecera() {
        cabecera.getChildren().clear();
        cabecera.getStyleClass().setAll("cabecera-ficha");

        Button volver = Ui.botonIcono(Feather.ARROW_LEFT, "Volver a pacientes");
        volver.setOnAction(e -> navegador.irA(Navegador.Seccion.PACIENTES));

        Label nombre = Ui.etiqueta(paciente.nombreCompleto(), "ficha-nombre");
        List<String> datos = new ArrayList<>();
        datos.add("DNI " + paciente.getDni());
        if (paciente.edad() != null) {
            datos.add(paciente.edad() + " años");
        }
        if (paciente.getSexo() != null) {
            datos.add(switch (paciente.getSexo()) {
                case "F" -> "Femenino";
                case "M" -> "Masculino";
                default -> "Otro";
            });
        }
        if (paciente.getTelefono() != null) {
            datos.add(paciente.getTelefono());
        }
        Label sub = Ui.etiqueta(String.join("  ·  ", datos), "texto-tenue");
        HBox alertas = new HBox(6);
        alertas.setAlignment(Pos.CENTER_LEFT);
        if (!paciente.isActivo()) {
            alertas.getChildren().add(Ui.chip("Archivado", "neutro", Feather.ARCHIVE));
        }
        if (paciente.getAlergias() != null) {
            Label alergia = Ui.chip("Alergia: " + paciente.getAlergias(), "peligro", Feather.ALERT_OCTAGON);
            // La alergia es información crítica: nunca se recorta.
            alergia.setMinWidth(Region.USE_PREF_SIZE);
            alertas.getChildren().add(alergia);
        }
        if (paciente.getAntecedentes() != null) {
            Label a = Ui.chip(paciente.getAntecedentes(), "alerta", Feather.HEART);
            a.setMaxWidth(340);
            Ui.tooltip(a, paciente.getAntecedentes());
            alertas.getChildren().add(a);
        }
        nombre.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaSub = new HBox(10, sub);
        lineaSub.setAlignment(Pos.CENTER_LEFT);
        VBox textos = new VBox(3, new HBox(10, nombre, alertas), lineaSub);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);
        ((HBox) textos.getChildren().get(0)).setAlignment(Pos.CENTER_LEFT);

        tabOdontograma = pestana("Odontograma 3D", Feather.BOX, () -> cuerpo.getChildren().setAll(pestanaOdontograma));
        ToggleButton tabPlan = pestana("Plan y presupuesto", Feather.CLIPBOARD, () -> {
            refrescarPlan();
            cuerpo.getChildren().setAll(pestanaPlan);
        });
        ToggleButton tabHist = pestana("Historial", Feather.CLOCK, () -> {
            refrescarHistorial();
            cuerpo.getChildren().setAll(pestanaHistorial);
        });
        tabOdontograma.setSelected(true);
        HBox pestanas = new HBox(tabOdontograma, tabPlan, tabHist);
        pestanas.getStyleClass().add("pestanas");

        Button editar = Ui.botonIcono(Feather.EDIT_2, "Editar datos del paciente");
        editar.setOnAction(e -> FormularioPaciente.abrir(notificador, pacientes, paciente, guardado -> {
            paciente = guardado;
            construirCabecera();
        }));
        Button exportar = Ui.primario("Informe clínico", Feather.DOWNLOAD);
        Ui.tooltip(exportar, "PDF con el odontograma 3D, los hallazgos y las consultas (el presupuesto está en su pestaña)");
        exportar.setOnAction(e -> exportarInforme());

        boolean esPaciente = sesion.esPaciente();
        Button cita = esPaciente ? Ui.secundario("Pedir cita", Feather.CALENDAR)
                : Ui.botonIcono(Feather.CALENDAR, "Agendar cita");
        cita.setOnAction(e -> dialogoCita.abrir(new DialogoCita.Sugerencia(paciente, null, null, null), c -> construirCabecera()));
        citas.proxima(paciente).ifPresent(c -> {
            Label chip = Ui.chip("Cita " + c.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                    + (c.getEstado() == com.dentavista.modelo.EstadoCita.SOLICITADA ? " (por confirmar)" : ""),
                    "info", Feather.CALENDAR);
            Ui.tooltip(chip, "Próxima cita: " + com.dentavista.ui.Formato.fechaHora(c.getInicio()) + " con "
                    + c.getOdontologo().getNombre());
            chip.setMinWidth(Region.USE_PREF_SIZE);
            lineaSub.getChildren().add(chip);
        });

        if (esPaciente) {
            cabecera.getChildren().addAll(Ui.avatar(paciente.iniciales(), paciente.getDni(), 46), textos,
                    pestanas, cita, exportar);
            fijarAncho(pestanas, cita, exportar);
            return;
        }
        Button receta = Ui.botonIcono(Feather.FILE_PLUS, "Emitir receta (verifica alergias y antecedentes)");
        receta.setOnAction(e -> dialogoReceta.abrir(paciente, () -> {
            if (pestanaHistorial != null && cuerpo.getChildren().contains(pestanaHistorial)) {
                refrescarHistorial();
            }
        }));
        receta.setVisible(sesion.puedeRegistrarClinica());
        receta.setManaged(sesion.puedeRegistrarClinica());
        Button acceso = Ui.botonIcono(Feather.KEY, "Acceso del paciente (usuario para ver su odontograma y pedir citas)");
        acceso.setOnAction(e -> dialogoAcceso());
        Button baja = paciente.isActivo() ? Ui.botonIcono(Feather.ARCHIVE, "Archivar (dar de baja): ya no necesita atención")
                : Ui.botonIcono(Feather.ROTATE_CCW, "Reactivar paciente");
        baja.setOnAction(e -> archivarOReactivar());
        cabecera.getChildren().addAll(volver, Ui.avatar(paciente.iniciales(), paciente.getDni(), 46), textos,
                pestanas, acceso, editar, baja, receta, cita, exportar);
        fijarAncho(pestanas, acceso, editar, baja, receta, cita, exportar);
    }

    private void archivarOReactivar() {
        if (!paciente.isActivo()) {
            if (notificador.intentar(() -> paciente = pacientes.reactivar(paciente))) {
                notificador.exito(paciente.nombreCompleto() + " vuelve a estar activo.");
                construirCabecera();
            }
            return;
        }
        Label info = Ui.etiqueta(paciente.nombreCompleto() + " dejará de aparecer en la lista, la agenda y los avisos. "
                + "Su historia clínica se conserva y puedes reactivarlo cuando vuelva. "
                + "Sus citas por venir se cancelan y su acceso queda desactivado.", "texto-tenue");
        info.setWrapText(true);
        notificador.dialogo(Feather.ARCHIVE, "Archivar paciente", new VBox(info), "Archivar", true, () -> {
            pacientes.archivar(paciente);
            notificador.exito(paciente.nombreCompleto() + " fue archivado.");
            navegador.irA(Navegador.Seccion.PACIENTES);
        }, 480);
    }

    /** Botones y pestañas nunca se recortan: si falta espacio se encoge el nombre del paciente. */
    private static void fijarAncho(Region... nodos) {
        for (Region r : nodos) {
            r.setMinWidth(Region.USE_PREF_SIZE);
            if (r instanceof HBox h) {
                h.getChildren().forEach(n -> ((Region) n).setMinWidth(Region.USE_PREF_SIZE));
            }
        }
    }

    private ToggleButton pestana(String texto, Feather icono, Runnable accion) {
        ToggleButton t = new ToggleButton(texto, Ui.icono(icono, 15));
        t.setToggleGroup(grupoPestanas);
        t.setOnAction(e -> {
            if (!t.isSelected()) {
                t.setSelected(true);
            }
            accion.run();
        });
        return t;
    }

    // ============================================================ odontograma

    private Node construirOdontograma() {
        visor = new VisorDental3D();
        visor.setAlPulsar(this::alPulsarPieza);
        visor.setAlDobleClic(this::volverSana);

        o2d = new Odontograma2D();
        o2d.setAlPulsar(this::alPulsarPieza);
        o2d.setAlDobleClic(this::volverSana);
        o2d.setPrefHeight(210);
        o2d.setMinHeight(150);

        escenario = new StackPane(visor, barra3D(), lineaTiempo(), aviso());
        escenario.getStyleClass().add("escenario-3d");
        escenario.setMinHeight(300);
        VBox.setVgrow(escenario, Priority.ALWAYS);
        // Recorta el 3D a las esquinas redondeadas del escenario.
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(escenario.widthProperty());
        clip.heightProperty().bind(escenario.heightProperty());
        escenario.setClip(clip);

        VBox tarjeta2d = Ui.tarjeta(o2d);
        tarjeta2d.setPadding(new Insets(8, 12, 8, 12));

        VBox centro = new VBox(12, escenario, tarjeta2d);
        HBox.setHgrow(centro, Priority.ALWAYS);

        ScrollPane scrollPieza = new ScrollPane(panelPieza);
        scrollPieza.setFitToWidth(true);
        scrollPieza.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPieza.setMinWidth(292);
        scrollPieza.setPrefWidth(292);
        panelPieza.getStyleClass().add("panel-pieza");

        // El paciente ve su boca en modo consulta: sin paleta de hallazgos.
        HBox h = sesion.esPaciente() ? new HBox(14, centro, scrollPieza) : new HBox(14, paleta(), centro, scrollPieza);
        h.setPadding(new Insets(14, 16, 14, 16));
        return h;
    }

    /** Paleta de hallazgos: elegir uno y hacer clic en la cara del diente. */
    private Node paleta() {
        VBox v = new VBox(3);
        v.getStyleClass().add("paleta");
        v.getChildren().add(Ui.etiqueta("HERRAMIENTA", "campo-rotulo"));

        botonSeleccionar = new ToggleButton("Seleccionar", Ui.icono(Feather.MOUSE_POINTER, 15));
        botonSeleccionar.setToggleGroup(grupoHerramientas);
        botonSeleccionar.setSelected(true);
        botonSeleccionar.setUserData(null);
        v.getChildren().add(conAtajo(botonSeleccionar, "Esc"));

        v.getChildren().add(separadorPaleta("EN UNA CARA"));
        int tecla = 1;
        for (EstadoDental e : EstadoDental.values()) {
            if (e.seAplicaACara() && e != EstadoDental.SANO) {
                v.getChildren().add(botonHerramienta(e, tecla++));
            }
        }
        v.getChildren().add(separadorPaleta("PIEZA COMPLETA"));
        for (EstadoDental e : EstadoDental.values()) {
            if (e.getAlcance() == EstadoDental.Alcance.PIEZA) {
                v.getChildren().add(botonHerramienta(e, tecla <= 9 ? tecla++ : -1));
            }
        }
        v.getChildren().add(separadorPaleta("CORREGIR"));
        v.getChildren().add(botonHerramienta(EstadoDental.SANO, 0));

        grupoHerramientas.selectedToggleProperty().addListener((o, a, nuevo) -> {
            if (nuevo == null) {
                botonSeleccionar.setSelected(true);
                return;
            }
            herramienta = (EstadoDental) nuevo.getUserData();
            visor.setResaltarCara(herramienta == null || herramienta.seAplicaACara());
        });

        Button deshacer = Ui.secundario("Deshacer", Feather.ROTATE_CCW);
        deshacer.setMaxWidth(Double.MAX_VALUE);
        deshacer.setOnAction(e -> deshacer());
        Ui.tooltip(deshacer, "Deshace el último registro de hoy (Ctrl+Z)");

        estadoRegistro.getStyleClass().addAll("texto-tenue", "texto-pequeno");
        estadoRegistro.setWrapText(true);
        estadoRegistro.setText("Elige un hallazgo y haz clic en la cara del diente, en 3D o en el odontograma. "
                + "Doble clic en una cara la vuelve sana.");

        Region esp = new Region();
        VBox.setVgrow(esp, Priority.ALWAYS);
        v.getChildren().addAll(esp, deshacer, estadoRegistro);

        if (!sesion.puedeRegistrarClinica()) {
            botonesHerramienta.values().forEach(b -> b.setDisable(true));
            deshacer.setDisable(true);
            estadoRegistro.setText("Modo consulta: tu rol puede ver el odontograma pero no registrar hallazgos.");
        }
        ScrollPane s = new ScrollPane(v);
        s.setFitToWidth(true);
        s.setFitToHeight(true);
        s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        s.setMinWidth(208);
        return s;
    }

    private Label separadorPaleta(String texto) {
        Label l = Ui.etiqueta(texto, "campo-rotulo");
        l.setPadding(new Insets(10, 0, 2, 4));
        l.setStyle("-fx-font-size: 10px;");
        return l;
    }

    private Node botonHerramienta(EstadoDental e, int tecla) {
        ToggleButton b = new ToggleButton(e.getEtiqueta(), Ui.muestra(e.getColor()));
        b.setToggleGroup(grupoHerramientas);
        b.setUserData(e);
        botonesHerramienta.put(e, b);
        Ui.tooltip(b, ayudaEstado(e) + (tecla >= 0 ? "  (tecla " + tecla + ")" : ""));
        return conAtajo(b, tecla >= 0 ? String.valueOf(tecla) : "");
    }

    private Node conAtajo(ToggleButton b, String atajo) {
        Label l = Ui.etiqueta(atajo, "atajo");
        l.setMouseTransparent(true);
        StackPane s = new StackPane(b, l);
        StackPane.setAlignment(l, Pos.CENTER_RIGHT);
        StackPane.setMargin(l, new Insets(0, 10, 0, 0));
        return s;
    }

    private static String ayudaEstado(EstadoDental e) {
        return switch (e) {
            case SANO -> "Borra el hallazgo de la cara pulsada (o de toda la pieza si pulsas la silueta)";
            case CARIES -> "Lesión cariosa en la cara pulsada";
            case FRACTURA -> "Fractura o fisura en la cara pulsada";
            case OBTURADO -> "Restauración existente con resina";
            case SELLANTE -> "Sellante de fosas y fisuras";
            case ENDODONCIA_INDICADA -> "La pieza necesita tratamiento de conductos";
            case ENDODONCIA -> "Pieza con tratamiento de conductos";
            case EXTRACCION_INDICADA -> "La pieza debe extraerse";
            case CORONA -> "Pieza con corona protésica";
            case IMPLANTE -> "Implante dental en la posición";
            case AUSENTE -> "Pieza ausente o extraída";
        };
    }

    /** Barra flotante sobre el 3D: vistas, apertura de boca y opciones. */
    private Node barra3D() {
        Button frontal = boton3D("Frontal", Feather.SMILE, visor::vistaFrontal);
        Button sup = boton3D("Superior", Feather.CHEVRONS_UP, visor::vistaSuperior);
        Button inf = boton3D("Inferior", Feather.CHEVRONS_DOWN, visor::vistaInferior);
        // Laterales solo con ícono: la barra cabe completa también en pantallas de 1366 px.
        Button der = boton3D(null, Feather.CHEVRON_LEFT, visor::vistaDerecha);
        Button izq = boton3D(null, Feather.CHEVRON_RIGHT, visor::vistaIzquierda);
        Ui.tooltip(der, "Vista lateral: lado derecho del paciente");
        Ui.tooltip(izq, "Vista lateral: lado izquierdo del paciente");

        ToggleButton abrir = toggle3D("Abrir boca", Feather.MAXIMIZE_2, visor.bocaAbiertaProperty());
        ToggleButton numeros = toggle3D(null, Feather.HASH, visor.numerosVisiblesProperty());
        Ui.tooltip(numeros, "Mostrar números FDI");
        ToggleButton encias = toggle3D(null, Feather.LAYERS, visor.enciasVisiblesProperty());
        Ui.tooltip(encias, "Mostrar encías");
        ToggleButton giro = toggle3D(null, Feather.REFRESH_CW, visor.giroAutomaticoProperty());
        Ui.tooltip(giro, "Giro automático (modo presentación al paciente)");
        Button enfocar = boton3D(null, Feather.CROSSHAIR, () -> {
            if (piezaSel != null) {
                visor.enfocar(piezaSel);
            }
        });
        Ui.tooltip(enfocar, "Acercar a la pieza seleccionada");
        Ui.tooltip(botonPantallaCompleta, "Pantalla completa (F11) · Esc para salir");
        botonPantallaCompleta.setOnAction(e -> pantallaCompleta(botonPantallaCompleta.isSelected()));

        HBox h = new HBox(frontal, sup, inf, der, izq, sep(), abrir, sep(), numeros, encias, giro, enfocar, sep(),
                botonPantallaCompleta);
        // Barra adaptable: si el visor es angosto (pantallas de 1366/1440 px) los botones muestran
        // solo su ícono; la ayuda emergente sigue diciendo qué hace cada uno.
        Ui.tooltip(frontal, "Vista frontal");
        Ui.tooltip(sup, "Arcada superior (vista oclusal)");
        Ui.tooltip(inf, "Arcada inferior (vista oclusal)");
        Ui.tooltip(abrir, "Abrir la boca para ver las caras oclusales");
        visor.widthProperty().addListener((o, a, ancho) -> {
            boolean compacta = ancho.doubleValue() < 900;
            for (javafx.scene.control.ButtonBase b : List.of(frontal, sup, inf, abrir)) {
                b.setContentDisplay(compacta ? javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
                        : javafx.scene.control.ContentDisplay.LEFT);
            }
        });
        h.getStyleClass().add("barra-3d");
        h.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(h, Pos.TOP_CENTER);
        StackPane.setMargin(h, new Insets(12, 0, 0, 0));

        Label ayuda = Ui.etiqueta("Arrastra para girar · rueda para acercar · clic derecho para mover", "texto-pequeno");
        ayuda.setStyle("-fx-text-fill: rgba(203,213,225,0.55);");
        VBox caja = new VBox(6, h, ayuda);
        caja.setAlignment(Pos.TOP_CENTER);
        caja.setPickOnBounds(false);
        caja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(caja, Pos.TOP_CENTER);
        StackPane.setMargin(caja, new Insets(12, 0, 0, 0));
        return caja;
    }

    private Button boton3D(String texto, Feather icono, Runnable accion) {
        Button b = new Button(texto, Ui.icono(icono, 15));
        b.setOnAction(e -> accion.run());
        return b;
    }

    private ToggleButton toggle3D(String texto, Feather icono, javafx.beans.property.BooleanProperty prop) {
        ToggleButton t = new ToggleButton(texto, Ui.icono(icono, 15));
        t.selectedProperty().bindBidirectional(prop);
        return t;
    }

    private Region sep() {
        Region r = new Region();
        r.getStyleClass().add("separador");
        HBox.setMargin(r, new Insets(0, 4, 0, 4));
        return r;
    }

    private Node aviso() {
        avisoHistorico.setGraphic(Ui.icono(Feather.CLOCK, 14));
        avisoHistorico.getStyleClass().add("aviso-historico");
        avisoHistorico.setVisible(false);
        StackPane.setAlignment(avisoHistorico, Pos.TOP_CENTER);
        StackPane.setMargin(avisoHistorico, new Insets(86, 0, 0, 0));
        avisoHistorico.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return avisoHistorico;
    }

    // ------------------------------------------------------------ línea de tiempo

    /**
     * "Máquina del tiempo": cada marca es una consulta; al moverla, el odontograma
     * muestra la boca tal como estaba al terminar esa consulta. El botón ▶ reproduce
     * la evolución completa del paciente.
     */
    private Node lineaTiempo() {
        Button play = new Button(null, Ui.icono(Feather.PLAY, 14));
        Ui.tooltip(play, "Reproducir la evolución del paciente");
        play.setOnAction(e -> reproducir(play));

        linea.setMin(0);
        linea.setMajorTickUnit(1);
        linea.setMinorTickCount(0);
        linea.setSnapToTicks(true);
        linea.setShowTickMarks(true);
        linea.setShowTickLabels(false);
        linea.setBlockIncrement(1);
        HBox.setHgrow(linea, Priority.ALWAYS);
        linea.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double d) {
                int i = (int) Math.round(d);
                if (i >= cronologia.size()) {
                    return "Hoy";
                }
                return Formato.mesCorto(cronologia.get(i).getFecha());
            }

            @Override
            public Double fromString(String s) {
                return 0.0;
            }
        });
        linea.valueProperty().addListener((o, a, v) -> {
            if (!ajustandoLinea) {
                irAPunto((int) Math.round(v.doubleValue()));
            }
        });

        lblFecha.getStyleClass().add("fecha");
        lblFecha.setMinWidth(300);
        lblFecha.setPrefWidth(300);
        lblFecha.setMaxWidth(300);
        Label rotulo = new Label("LÍNEA DE TIEMPO", Ui.icono(Feather.CLOCK, 13));
        rotulo.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

        // Marcas con el mes de cada consulta, alineadas con las posiciones del deslizador.
        marcas.setMinHeight(16);
        marcas.setPrefHeight(16);
        marcas.widthProperty().addListener((o, a, b) -> dibujarMarcas());
        VBox deslizador = new VBox(2, linea, marcas);
        deslizador.setAlignment(Pos.CENTER);
        HBox.setHgrow(deslizador, Priority.ALWAYS);
        HBox h = new HBox(10, play, new VBox(1, rotulo, lblFecha), deslizador);
        h.getStyleClass().add("linea-tiempo");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(h, Pos.BOTTOM_CENTER);
        StackPane.setMargin(h, new Insets(0, 16, 14, 16));
        return h;
    }

    private void dibujarMarcas() {
        marcas.getChildren().clear();
        int n = cronologia.size();
        double w = marcas.getWidth();
        if (n == 0 || w <= 0) {
            return;
        }
        // El pulgar del deslizador mide ~16 px: los extremos quedan a 8 px del borde.
        double margen = 9;
        for (int i = 0; i <= n; i++) {
            Label l = new Label(i == n ? "Hoy" : Formato.mesCorto(cronologia.get(i).getFecha()));
            l.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c8aa3;");
            double centro = margen + (w - 2 * margen) * i / n;
            // El Pane da a cada etiqueta su tamaño preferido; se centra cuando ya lo conoce.
            l.layoutXProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> Math.max(0, Math.min(w - l.getWidth(), centro - l.getWidth() / 2)), l.widthProperty()));
            marcas.getChildren().add(l);
        }
    }

    private void configurarLinea() {
        ajustandoLinea = true;
        int n = cronologia.size();
        linea.setMax(Math.max(1, n));
        linea.setValue(n);
        linea.setDisable(n == 0);
        ajustandoLinea = false;
        actualizarRotuloFecha(n);
        dibujarMarcas();
    }

    private void irAPunto(int i) {
        int n = cronologia.size();
        if (i >= n) {
            fechaVista = null;
        } else if (i + 1 < n) {
            // Estado al cerrar la consulta i: todo lo registrado antes de la siguiente.
            fechaVista = cronologia.get(i + 1).getFecha().minusSeconds(1);
        } else {
            fechaVista = cronologia.get(i).getFecha().plusHours(12);
        }
        actualizarRotuloFecha(i);
        refrescarOdontograma();
    }

    private void actualizarRotuloFecha(int i) {
        int n = cronologia.size();
        if (i >= n) {
            lblFecha.setText("Hoy · estado actual");
            avisoHistorico.setVisible(false);
        } else {
            Consulta c = cronologia.get(i);
            lblFecha.setText(Formato.fecha(c.getFecha()) + " · " + c.getMotivo());
            avisoHistorico.setText("Viendo el " + Formato.fecha(c.getFecha()) + " (consulta " + (i + 1) + " de " + n
                    + ") · solo lectura");
            avisoHistorico.setVisible(true);
        }
    }

    private void reproducir(Button play) {
        if (reproduccion != null) {
            reproduccion.stop();
            reproduccion = null;
            play.setGraphic(Ui.icono(Feather.PLAY, 14));
            return;
        }
        int n = cronologia.size();
        if (n == 0) {
            notificador.info("Este paciente aún no tiene consultas registradas.");
            return;
        }
        play.setGraphic(Ui.icono(Feather.PAUSE, 14));
        visor.vistaFrontal();
        reproduccion = new Timeline();
        for (int i = 0; i <= n; i++) {
            final int punto = i;
            reproduccion.getKeyFrames().add(new KeyFrame(Duration.millis(300 + i * 1300L), e -> linea.setValue(punto)));
        }
        reproduccion.setOnFinished(e -> {
            reproduccion = null;
            play.setGraphic(Ui.icono(Feather.PLAY, 14));
        });
        reproduccion.play();
    }

    // ------------------------------------------------------------- interacción

    private void alPulsarPieza(Integer fdi, Superficie cara) {
        if (fdi == null) {
            return;
        }
        seleccionar(fdi, cara);
        if (herramienta == null) {
            return;
        }
        if (fechaVista != null) {
            notificador.info("Estás viendo una fecha pasada. Mueve la línea de tiempo a \"Hoy\" para registrar.");
            return;
        }
        aplicar(fdi, cara, herramienta);
    }

    /**
     * Doble clic: la cara pulsada vuelve a estar sana (o la pieza completa si se pulsó la
     * silueta o si la pieza estaba ausente, con corona o implante). Si el primer clic del
     * doble clic acaba de registrar un hallazgo con la herramienta activa, ese registro se
     * deshace en lugar de dejar "caries" y "sano" seguidos en la historia.
     */
    private void volverSana(Integer fdi, Superficie cara) {
        if (fdi == null) {
            return;
        }
        if (!sesion.puedeRegistrarClinica()) {
            notificador.info("Tu rol solo puede consultar el odontograma.");
            return;
        }
        if (fechaVista != null) {
            notificador.info("Estás viendo una fecha pasada. Mueve la línea de tiempo a \"Hoy\" para registrar.");
            return;
        }
        seleccionar(fdi, cara);
        if (ultimoClic != null && ultimoClic.getPieza() == fdi && System.nanoTime() - ultimoClicNanos < 800_000_000L) {
            notificador.intentar(() -> clinica.deshacerUltimo(paciente));
        }
        ultimoClic = null;
        Odontograma.EstadoPieza estado = clinica.odontograma(paciente).pieza(fdi);
        boolean piezaCompleta = cara == Superficie.PIEZA
                || (estado.general() != null && estado.general().anulaCaras());
        Superficie destino = piezaCompleta ? Superficie.PIEZA : cara;
        aplicar(fdi, destino, EstadoDental.SANO);
        if (estadoRegistro.getText().startsWith("Sin cambios")) {
            estadoRegistro.setText("La " + (piezaCompleta ? "pieza " + fdi : "cara " + destino.etiqueta(fdi).toLowerCase()
                    + " de la pieza " + fdi) + " ya está sana.");
            cronologia = clinica.consultasCronologicas(paciente);
            configurarLinea();
            refrescarOdontograma();
        }
    }

    private Hallazgo ultimoClic;
    private long ultimoClicNanos;

    private void aplicar(int fdi, Superficie cara, EstadoDental estado) {
        Superficie destino;
        if (estado == EstadoDental.SANO) {
            destino = cara;
        } else if (estado.getAlcance() == EstadoDental.Alcance.PIEZA) {
            destino = Superficie.PIEZA;
        } else if (cara == Superficie.PIEZA) {
            notificador.advertencia(estado.getEtiqueta() + " se marca en una cara: haz clic en el cuadro de caras o en la cara del diente 3D.");
            return;
        } else {
            destino = cara;
        }
        try {
            Hallazgo h = clinica.registrar(paciente, fdi, destino, estado, nota.getText());
            if (h == null) {
                estadoRegistro.setText("Sin cambios: " + fdi + " " + destino.etiqueta(fdi).toLowerCase()
                        + " ya estaba como " + estado.getEtiqueta().toLowerCase() + ".");
                return;
            }
            nota.clear();
            ultimoClic = estado == EstadoDental.SANO ? null : h;
            ultimoClicNanos = System.nanoTime();
            estadoRegistro.setText("✔ Registrado: " + estado.getEtiqueta() + " · pieza " + fdi
                    + (destino.esCara() ? " " + destino.etiqueta(fdi).toLowerCase() : ""));
            boolean nuevaConsulta = cronologia.stream().noneMatch(c -> c.getId().equals(h.getConsulta().getId()));
            if (nuevaConsulta) {
                cronologia = clinica.consultasCronologicas(paciente);
                configurarLinea();
            }
            refrescarOdontograma();
        } catch (NegocioException e) {
            notificador.advertencia(e.getMessage());
        }
    }

    private void deshacer() {
        notificador.intentar(() -> {
            Hallazgo h = clinica.deshacerUltimo(paciente);
            if (h == null) {
                notificador.info("No hay registros de hoy para deshacer.");
                return;
            }
            estadoRegistro.setText("↶ Deshecho: " + h.getEstado().getEtiqueta() + " · pieza " + h.getPieza());
            cronologia = clinica.consultasCronologicas(paciente);
            configurarLinea();
            seleccionar(h.getPieza(), h.getSuperficie() == Superficie.PIEZA ? null : h.getSuperficie());
            refrescarOdontograma();
        });
    }

    private void seleccionar(Integer fdi, Superficie cara) {
        piezaSel = fdi;
        caraSel = cara == Superficie.PIEZA ? null : cara;
        visor.seleccionar(fdi);
        o2d.seleccionar(fdi);
        refrescarPanelPieza();
    }

    private Integer primeraPiezaConHallazgo() {
        return actual.patologias().stream().map(Odontograma.Patologia::pieza).findFirst().orElse(16);
    }

    private void instalarAtajos() {
        atajos = e -> {
            if (e.getTarget() instanceof TextInputControl || !tabOdontograma.isSelected()) {
                return;
            }
            if (e.getCode() == KeyCode.F11) {
                pantallaCompleta(ventanaCompleta == null);
                e.consume();
                return;
            }
            if (e.isShortcutDown() && e.getCode() == KeyCode.Z) {
                deshacer();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                botonSeleccionar.setSelected(true);
                e.consume();
                return;
            }
            if (e.getCode().isDigitKey() && sesion.puedeRegistrarClinica()) {
                int d = Integer.parseInt(e.getText().isBlank() ? "-1" : e.getText());
                List<ToggleButton> orden = new ArrayList<>(botonesHerramienta.values());
                // Orden de teclas: 1..n según la paleta; 0 = Sano.
                ToggleButton b = d == 0 ? botonesHerramienta.get(EstadoDental.SANO)
                        : d - 1 < orden.size() && orden.get(d - 1).getUserData() != EstadoDental.SANO ? orden.get(d - 1) : null;
                if (b != null) {
                    b.setSelected(true);
                    e.consume();
                }
            }
        };
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, atajos);
    }

    // -------------------------------------------------------- pantalla completa

    /**
     * Lleva el escenario 3D (con su barra y la línea de tiempo) a una ventana a pantalla
     * completa, ideal para explicarle el diagnóstico al paciente. Al salir (Esc, F11 o el
     * botón) vuelve exactamente a su lugar en la ficha.
     */
    private void pantallaCompleta(boolean activar) {
        if (!activar) {
            if (ventanaCompleta != null) {
                ventanaCompleta.close();
            }
            return;
        }
        if (ventanaCompleta != null) {
            return;
        }
        javafx.scene.layout.Pane padre = (javafx.scene.layout.Pane) escenario.getParent();
        int posicion = padre.getChildren().indexOf(escenario);
        Node clip = escenario.getClip();
        padre.getChildren().remove(escenario);
        escenario.setClip(null);

        javafx.scene.Scene principal = JavaFxApplication.get().ventana().getScene();
        StackPane contenedor = new StackPane(escenario);
        contenedor.getStyleClass().setAll(principal.getRoot().getStyleClass());
        javafx.scene.Scene escena = new javafx.scene.Scene(contenedor);
        escena.getStylesheets().setAll(principal.getStylesheets());
        if (atajos != null) {
            escena.addEventFilter(KeyEvent.KEY_PRESSED, atajos);
        }

        ventanaCompleta = new javafx.stage.Stage();
        ventanaCompleta.initOwner(JavaFxApplication.get().ventana());
        ventanaCompleta.setTitle(paciente.nombreCompleto() + " · Odontograma 3D");
        ventanaCompleta.getIcons().setAll(JavaFxApplication.get().ventana().getIcons());
        ventanaCompleta.setScene(escena);
        ventanaCompleta.setFullScreenExitHint("Pulsa Esc para salir de la pantalla completa");
        ventanaCompleta.setFullScreen(true);
        ventanaCompleta.fullScreenProperty().addListener((o, a, completa) -> {
            if (!completa && ventanaCompleta != null) {
                ventanaCompleta.close();
            }
        });
        ventanaCompleta.setOnHidden(e -> {
            contenedor.getChildren().remove(escenario);
            escenario.setClip(clip);
            padre.getChildren().add(posicion, escenario);
            ventanaCompleta = null;
            botonPantallaCompleta.setSelected(false);
            botonPantallaCompleta.setGraphic(Ui.icono(Feather.MAXIMIZE, 15));
        });
        botonPantallaCompleta.setSelected(true);
        botonPantallaCompleta.setGraphic(Ui.icono(Feather.MINIMIZE, 15));
        ventanaCompleta.show();
    }

    // ------------------------------------------------------------- refrescos

    private void recargarTodo() {
        cronologia = clinica.consultasCronologicas(paciente);
        configurarLinea();
        fechaVista = null;
        refrescarOdontograma();
    }

    private void refrescarOdontograma() {
        actual = clinica.odontograma(paciente, fechaVista);
        visor.setOdontograma(actual);
        o2d.setOdontograma(actual);
        refrescarPanelPieza();
    }

    // --------------------------------------------------------- panel de pieza

    private void refrescarPanelPieza() {
        panelPieza.getChildren().clear();
        if (piezaSel == null) {
            panelPieza.getChildren().add(Ui.vacio(Feather.MOUSE_POINTER, "Selecciona una pieza",
                    "Haz clic sobre un diente en el modelo 3D o en el odontograma."));
            return;
        }
        int fdi = piezaSel;
        Odontograma.EstadoPieza e = actual.pieza(fdi);

        Label numero = Ui.etiqueta(String.valueOf(fdi), "pieza-numero");
        numero.setMinWidth(Region.USE_PREF_SIZE);
        Label nombre = Ui.etiqueta(Pieza.nombre(fdi), "pieza-nombre");
        Label estado = e.sana() ? Ui.chip("Sana", "exito", Feather.CHECK)
                : e.ausente() ? Ui.chip("Ausente", "neutro", Feather.MINUS_CIRCLE)
                : tienePatologia(e) ? Ui.chip("Requiere tratamiento", "peligro", Feather.ALERT_CIRCLE)
                : Ui.chip("Tratada", "info", Feather.SHIELD);
        VBox titulo = new VBox(2, nombre, estado);
        HBox cab = new HBox(12, numero, titulo);
        cab.setAlignment(Pos.CENTER_LEFT);

        // Diagrama grande de las cinco caras (clicable).
        javafx.scene.Group diagrama = Odontograma2D.diagramaPieza(fdi, e, 116, caraSel,
                (p, cara) -> alPulsarPieza(p, cara), this::volverSana);
        StackPane cajaDiagrama = new StackPane(diagrama);
        cajaDiagrama.setPadding(new Insets(14));
        cajaDiagrama.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 12;");

        VBox caras = new VBox(0);
        for (Superficie s : Superficie.CARAS) {
            EstadoDental v = e.visible(s);
            Label l = Ui.etiqueta(s.etiqueta(fdi), s == caraSel ? "texto-fuerte" : "");
            Label est = Ui.etiqueta(e.ausente() ? "—" : v.getEtiqueta(), "texto-tenue");
            HBox fila = new HBox(8, Ui.muestra(e.ausente() ? "#e2e8f0" : v.getColor()), l, Ui.espaciador(), est);
            fila.getStyleClass().add("fila-cara");
            caras.getChildren().add(fila);
        }
        if (e.general() != null) {
            HBox fila = new HBox(8, Ui.muestra(e.general().getColor()), Ui.etiqueta("Pieza completa", "texto-fuerte"),
                    Ui.espaciador(), Ui.etiqueta(e.general().getEtiqueta(), "texto-tenue"));
            fila.getStyleClass().add("fila-cara");
            caras.getChildren().add(fila);
        }

        Button enfocar = Ui.secundario("Ver de cerca", Feather.CROSSHAIR);
        enfocar.setOnAction(ev -> visor.enfocar(fdi));
        Button tratar = Ui.secundario("Tratamiento", Feather.PLUS);
        tratar.setOnAction(ev -> dialogoAgregarTratamiento(fdi, caraSel));
        tratar.setDisable(!sesion.puedeRegistrarClinica());
        tratar.setVisible(!sesion.esPaciente());
        tratar.setManaged(!sesion.esPaciente());
        HBox acciones = new HBox(8, enfocar, tratar);
        enfocar.setMaxWidth(Double.MAX_VALUE);
        tratar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(enfocar, Priority.ALWAYS);
        HBox.setHgrow(tratar, Priority.ALWAYS);

        nota.setPromptText("Nota clínica para el próximo registro (opcional)");
        nota.setPrefRowCount(2);
        nota.setWrapText(true);
        nota.setDisable(!sesion.puedeRegistrarClinica() || fechaVista != null);
        nota.setVisible(!sesion.esPaciente());
        nota.setManaged(!sesion.esPaciente());

        VBox historial = new VBox(6);
        List<Hallazgo> hs = clinica.historialPieza(paciente, fdi);
        if (hs.isEmpty()) {
            historial.getChildren().add(Ui.etiqueta("Sin registros en esta pieza.", "texto-tenue"));
        }
        for (Hallazgo h : hs.stream().limit(12).toList()) {
            boolean futuro = fechaVista != null && h.getFecha().isAfter(fechaVista);
            Label t = new Label(h.getEstado().getEtiqueta()
                    + (h.getSuperficie().esCara() ? " · " + h.getSuperficie().etiqueta(fdi) : " · pieza"),
                    Ui.muestra(h.getEstado().getColor()));
            t.getStyleClass().add("texto-fuerte");
            t.setGraphicTextGap(8);
            Label d = Ui.etiqueta(Formato.fecha(h.getFecha()) + " · " + (h.getRegistradoPor() == null ? "" : h.getRegistradoPor()),
                    "texto-tenue", "texto-pequeno");
            VBox item = new VBox(2, t, d);
            if (h.getNota() != null) {
                Label n = Ui.etiqueta("“" + h.getNota() + "”", "texto-pequeno");
                n.setWrapText(true);
                item.getChildren().add(n);
            }
            item.getStyleClass().add("historial-item");
            if (futuro) {
                item.setOpacity(0.4);
            }
            historial.getChildren().add(item);
        }

        Button completo = Ui.secundario("Historial completo de la pieza", Feather.CLOCK);
        completo.setMaxWidth(Double.MAX_VALUE);
        Ui.tooltip(completo, "Evolución de la pieza " + fdi + " consulta por consulta, con PDF");
        completo.setOnAction(ev -> dialogoHistorialPieza.abrir(paciente, fdi));

        panelPieza.getChildren().addAll(cab, cajaDiagrama, Ui.etiqueta("CARAS", "campo-rotulo"), caras,
                acciones, nota, Ui.etiqueta("HISTORIA DE LA PIEZA", "campo-rotulo"), historial, completo);
    }

    private static boolean tienePatologia(Odontograma.EstadoPieza e) {
        return (e.general() != null && e.general().esPatologia())
                || e.caras().values().stream().anyMatch(EstadoDental::esPatologia);
    }

    // ===================================================================== plan

    private Node construirPlan() {
        Label titulo = Ui.etiqueta("Plan de tratamiento", "tarjeta-titulo");
        Label sub = Ui.etiqueta("Genera el plan desde el odontograma, marca lo realizado y entrega el presupuesto.",
                "tarjeta-subtitulo");
        Button generar = Ui.primario("Generar desde odontograma", Feather.ZAP);
        generar.setOnAction(e -> notificador.intentar(() -> {
            int n = planes.generarDesdeOdontograma(paciente);
            if (n == 0) {
                notificador.info("El plan ya cubre todas las patologías del odontograma.");
            } else {
                notificador.exito(n + (n == 1 ? " tratamiento agregado" : " tratamientos agregados") + " al plan.");
            }
            refrescarPlan();
        }));
        Button agregar = Ui.secundario("Agregar tratamiento", Feather.PLUS);
        agregar.setOnAction(e -> dialogoAgregarTratamiento(piezaSel, caraSel));
        Button exportar = Ui.secundario("Presupuesto PDF", Feather.DOWNLOAD);
        Ui.tooltip(exportar, "PDF con los tratamientos, importes, lo pagado y el saldo");
        exportar.setOnAction(e -> exportarPresupuesto());
        boolean puede = sesion.puedeRegistrarClinica();
        generar.setDisable(!puede);
        agregar.setDisable(!puede);
        HBox botonesPlan = new HBox(8, generar, agregar, exportar);
        botonesPlan.getChildren().forEach(n -> ((Region) n).setMinWidth(Region.USE_PREF_SIZE));
        VBox cab = new VBox(10, new VBox(2, titulo, sub), botonesPlan);
        cab.setAlignment(Pos.CENTER_LEFT);

        configurarTablaPlan();
        VBox.setVgrow(tablaPlan, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(cab, totalesPlan, tablaPlan);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        ScrollPane lateral = new ScrollPane(panelCuenta);
        lateral.setFitToWidth(true);
        lateral.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lateral.setMinWidth(340);
        lateral.setPrefWidth(340);
        HBox v = new HBox(16, tarjeta, lateral);
        v.setPadding(new Insets(16));
        return v;
    }

    private void configurarTablaPlan() {
        tablaPlan.getStyleClass().addAll("tabla");
        tablaPlan.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPlan.setPlaceholder(Ui.vacio(Feather.CLIPBOARD, "Plan vacío",
                "Registra hallazgos en el odontograma y pulsa \"Generar desde odontograma\"."));

        TableColumn<ItemPlan, ItemPlan> pieza = col("Pieza", i -> i);
        pieza.setPrefWidth(170);
        pieza.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                Label n = Ui.etiqueta(i.ubicacion(), "texto-fuerte");
                Label d = Ui.etiqueta(i.descripcionPieza(), "texto-tenue", "texto-pequeno");
                setGraphic(new VBox(1, n, d));
            }
        });
        TableColumn<ItemPlan, ItemPlan> trat = col("Tratamiento", i -> i);
        trat.setPrefWidth(260);
        trat.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                Label n = Ui.etiqueta(i.getTratamiento().getNombre(), "texto-fuerte");
                VBox v = new VBox(1, n);
                if (i.getNota() != null) {
                    v.getChildren().add(Ui.etiqueta(i.getNota(), "texto-tenue", "texto-pequeno"));
                }
                setGraphic(v);
            }
        });
        TableColumn<ItemPlan, String> fecha = col("Fecha", i -> i.getEstado() == EstadoItem.REALIZADO
                ? "Realizado " + Formato.fecha(i.getRealizado()) : "Plan " + Formato.fecha(i.getCreado()));
        fecha.setPrefWidth(150);
        TableColumn<ItemPlan, ItemPlan> estado = col("Estado", i -> i);
        estado.setPrefWidth(124);
        estado.setMinWidth(124);
        estado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(switch (i.getEstado()) {
                    case PENDIENTE -> Ui.chip(EstadoItem.PENDIENTE.getEtiqueta(), "alerta", Feather.CLOCK);
                    case REALIZADO -> Ui.chip("Realizado", "exito", Feather.CHECK);
                    case CANCELADO -> Ui.chip("Cancelado", "neutro", Feather.X);
                });
            }
        });
        TableColumn<ItemPlan, String> precio = col("Importe", i -> Formato.soles(i.getPrecio()));
        precio.setPrefWidth(110);
        precio.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<ItemPlan, ItemPlan> acciones = col("", i -> i);
        acciones.setPrefWidth(190);
        acciones.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ItemPlan i, boolean vacio) {
                super.updateItem(i, vacio);
                if (vacio || i == null) {
                    setGraphic(null);
                    return;
                }
                HBox h = new HBox(6);
                h.setAlignment(Pos.CENTER_RIGHT);
                boolean puede = sesion.puedeRegistrarClinica();
                if (i.getEstado() == EstadoItem.PENDIENTE) {
                    Button hecho = Ui.botonIcono(Feather.CHECK_CIRCLE, "Marcar como realizado");
                    hecho.setDisable(!puede);
                    hecho.setOnAction(e -> realizar(i));
                    Button cancelar = Ui.botonIcono(Feather.X, "Cancelar tratamiento");
                    cancelar.setDisable(!puede);
                    cancelar.setOnAction(e -> notificador.intentar(() -> {
                        planes.cancelar(i);
                        refrescarPlan();
                    }));
                    Button agendar = Ui.botonIcono(Feather.CALENDAR,
                            sesion.esPaciente() ? "Pedir cita para este tratamiento" : "Agendar cita para este tratamiento");
                    agendar.setOnAction(e -> dialogoCita.abrir(new DialogoCita.Sugerencia(paciente, null, null, i),
                            c -> construirCabecera()));
                    if (sesion.esPaciente()) {
                        h.getChildren().add(agendar);
                    } else {
                        h.getChildren().addAll(agendar, hecho, cancelar);
                    }
                } else if (i.getEstado() == EstadoItem.CANCELADO) {
                    Button reabrir = Ui.botonIcono(Feather.ROTATE_CCW, "Volver a pendiente");
                    reabrir.setDisable(!puede);
                    reabrir.setOnAction(e -> notificador.intentar(() -> {
                        planes.reabrir(i);
                        refrescarPlan();
                    }));
                    h.getChildren().add(reabrir);
                }
                if (i.getPieza() != null) {
                    Button ver = Ui.botonIcono(Feather.EYE, "Ver la pieza en el odontograma");
                    ver.setOnAction(e -> {
                        tabOdontograma.fire();
                        seleccionar(i.getPieza(), i.getSuperficie());
                        visor.enfocar(i.getPieza());
                    });
                    h.getChildren().add(ver);
                }
                setGraphic(h);
            }
        });
        tablaPlan.getColumns().setAll(List.of(pieza, trat, fecha, estado, precio, acciones));
    }

    private void realizar(ItemPlan i) {
        notificador.confirmar("Marcar como realizado",
                i.getTratamiento().getNombre() + " en " + i.ubicacion() + ".\n"
                        + (i.getTratamiento().getEstadoResultante() != null && i.getPieza() != null
                        ? "El odontograma se actualizará: la pieza quedará como \""
                        + i.getTratamiento().getEstadoResultante().getEtiqueta().toLowerCase() + "\"." : ""),
                "Confirmar", false, () -> notificador.intentar(() -> {
                    planes.realizar(i);
                    notificador.exito(i.getTratamiento().getNombre() + " registrado como realizado.");
                    cronologia = clinica.consultasCronologicas(paciente);
                    configurarLinea();
                    fechaVista = null;
                    refrescarOdontograma();
                    refrescarPlan();
                }));
    }

    // ================================================================= pagos

    /** Estado de cuenta, pagos y botón para cobrar. */
    private void refrescarCuenta() {
        panelCuenta.getChildren().clear();
        PagoService.Cuenta c = pagos.cuenta(paciente);
        // Un solo número protagonista: lo que debe hoy, o lo que falta pagar del plan.
        boolean debe = c.deuda().signum() > 0;
        boolean cancelado = c.porPagarDelPlan().signum() == 0 && c.totalPlan().signum() > 0;
        String rotulo = debe ? "DEBE (TRATAMIENTOS YA REALIZADOS)" : cancelado ? "PLAN PAGADO POR COMPLETO" : "POR PAGAR DEL PLAN";
        Label saldo = Ui.etiqueta(Formato.soles(debe ? c.deuda() : cancelado ? c.pagado() : c.porPagarDelPlan()), "total-valor");
        saldo.setStyle(debe ? "-fx-text-fill: #dc2626;" : cancelado ? "-fx-text-fill: #16a34a;" : "");
        String detalle = "Pagado " + Formato.soles(c.pagado()) + " de " + Formato.soles(c.totalPlan())
                + (debe || cancelado ? "" : c.aFavor().signum() > 0 ? " · incluye adelanto" : " · al día");
        VBox caja = new VBox(2, Ui.etiqueta(rotulo, "campo-rotulo"), saldo,
                Ui.etiqueta(detalle, "texto-tenue", "texto-pequeno"));
        caja.getStyleClass().add("total-caja");
        ProgressBar barra = new ProgressBar(c.avance());
        barra.getStyleClass().add("avance");
        barra.setMaxWidth(Double.MAX_VALUE);

        VBox tarjeta = Ui.tarjeta(Ui.cabecera("Pagos", Math.round(c.avance() * 100) + " % del plan pagado"), caja, barra);
        if (sesion.esPersonal()) {
            Button cobrar = Ui.primario("Registrar pago", Feather.DOLLAR_SIGN);
            cobrar.setMaxWidth(Double.MAX_VALUE);
            cobrar.setDisable(c.porPagarDelPlan().signum() == 0);
            cobrar.setOnAction(e -> dialogoPago(c));
            tarjeta.getChildren().add(cobrar);
        }
        VBox lista = new VBox(6);
        for (Pago pg : pagos.pagos(paciente)) {
            Label m = Ui.etiqueta(Formato.soles(pg.getMonto()), "texto-fuerte");
            if (pg.isAnulado()) {
                m.setStyle("-fx-strikethrough: true;");
            }
            Label d = Ui.etiqueta(pg.getRecibo() + " · " + Formato.fecha(pg.getFecha()) + " · " + pg.getMedio().getEtiqueta(),
                    "texto-tenue", "texto-pequeno");
            Button pdfRecibo = Ui.botonIcono(Feather.PRINTER, "Recibo en PDF");
            pdfRecibo.setOnAction(e -> notificador.intentar(() -> abrirArchivo(pdf.recibo(pg, pagos.cuenta(paciente)))));
            HBox fila = new HBox(8, new VBox(1, m, d), Ui.espaciador());
            if (pg.isAnulado()) {
                fila.getChildren().add(Ui.chip("Anulado", "neutro", null));
            } else if (sesion.esPersonal()) {
                Button anular = Ui.botonIcono(Feather.SLASH, "Anular recibo");
                anular.setOnAction(e -> dialogoAnular(pg));
                fila.getChildren().add(anular);
            }
            fila.getChildren().add(pdfRecibo);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("historial-item");
            lista.getChildren().add(fila);
        }
        if (lista.getChildren().isEmpty()) {
            lista.getChildren().add(Ui.etiqueta("Aún no hay pagos registrados.", "texto-tenue"));
        }
        panelCuenta.getChildren().addAll(tarjeta, Ui.tarjeta(Ui.cabecera("Historial de pagos", null), lista));
    }

    private void dialogoPago(PagoService.Cuenta c) {
        BigDecimal sugerido = c.deuda().signum() > 0 ? c.deuda() : c.porPagarDelPlan();
        javafx.scene.control.TextField monto = new javafx.scene.control.TextField(sugerido.toPlainString());
        com.dentavista.ui.Mascaras.dinero(monto);
        ToggleGroup g = new ToggleGroup();
        javafx.scene.layout.FlowPane medios = new javafx.scene.layout.FlowPane(6, 6);
        for (MedioPago m : MedioPago.values()) {
            ToggleButton b = new ToggleButton(m.getEtiqueta());
            b.setUserData(m);
            b.setToggleGroup(g);
            b.getStyleClass().add("hora-libre");
            if (m == MedioPago.EFECTIVO) {
                b.setSelected(true);
            }
            medios.getChildren().add(b);
        }
        javafx.scene.control.TextField nota = new javafx.scene.control.TextField();
        nota.setPromptText("Ej. N.º de operación Yape, primera cuota…");
        Label info = Ui.etiqueta("Debe por tratamientos ya realizados: " + Formato.soles(c.deuda())
                + " · Falta pagar del plan completo: " + Formato.soles(c.porPagarDelPlan()), "texto-tenue");
        info.setWrapText(true);
        VBox cont = new VBox(12, info, Ui.campo("Monto (S/)", monto), Ui.campo("Medio de pago", medios), Ui.campo("Nota", nota));
        notificador.dialogo(Feather.DOLLAR_SIGN, "Registrar pago", cont, "Registrar y emitir recibo", false, () -> {
            BigDecimal valor;
            try {
                valor = new BigDecimal(monto.getText().trim().replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new NegocioException("El monto debe ser un número, por ejemplo 150.00");
            }
            Pago pg = pagos.registrar(paciente, valor, g.getSelectedToggle() == null ? null
                    : (MedioPago) g.getSelectedToggle().getUserData(), nota.getText());
            notificador.exito("Pago registrado: recibo " + pg.getRecibo() + " por " + Formato.soles(pg.getMonto()) + ".");
            refrescarPlan();
            abrirArchivo(pdf.recibo(pg, pagos.cuenta(paciente)));
        }, 480);
    }

    private void dialogoAnular(Pago pg) {
        javafx.scene.control.TextField motivo = new javafx.scene.control.TextField();
        motivo.setPromptText("Ej. Monto mal digitado");
        VBox cont = new VBox(10, Ui.etiqueta("El recibo " + pg.getRecibo() + " por " + Formato.soles(pg.getMonto())
                + " dejará de contar como pagado. Queda en el historial como anulado.", "texto-tenue"), Ui.campo("Motivo", motivo));
        notificador.dialogo(Feather.SLASH, "Anular recibo", cont, "Anular", true, () -> {
            pagos.anular(pg, motivo.getText());
            notificador.exito("Recibo " + pg.getRecibo() + " anulado.");
            refrescarPlan();
        }, 460);
    }

    private void abrirArchivo(java.nio.file.Path archivo) {
        JavaFxApplication.get().hostServices().showDocument(archivo.toUri().toString());
    }

    private void refrescarPlan() {
        refrescarCuenta();
        List<ItemPlan> items = new ArrayList<>(planes.items(paciente));
        // Primero lo pendiente (lo que hay que hacer y cobrar), luego lo realizado más reciente.
        items.sort(java.util.Comparator.comparingInt((ItemPlan i) -> i.getEstado().ordinal())
                .thenComparing(i -> i.getEstado() == EstadoItem.REALIZADO ? i.getRealizado() : i.getCreado(),
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        tablaPlan.getItems().setAll(items);
        PlanService.Totales t = planes.totales(paciente);
        boolean pagado = pagos.cuenta(paciente).porPagarDelPlan().signum() == 0 && t.total().signum() > 0;
        ProgressBar barra = new ProgressBar(t.avance());
        barra.getStyleClass().add("avance");
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.setPrefHeight(8);
        VBox avance = new VBox(6, Ui.etiqueta("AVANCE DEL PLAN · " + Math.round(t.avance() * 100) + "%", "campo-rotulo"), barra);
        HBox.setHgrow(avance, Priority.ALWAYS);
        avance.setAlignment(Pos.CENTER_LEFT);
        HBox fila = new HBox(14,
                // Trabajo clínico (no dinero): lo cobrado y lo que falta pagar está en la tarjeta "Pagos".
                caja("Por realizar", Formato.soles(t.pendiente()),
                        t.itemsPendientes() + " tratamientos" + (pagado && t.itemsPendientes() > 0 ? " · ya pagados" : ""), true),
                caja("Realizado", Formato.soles(t.realizado()), t.itemsRealizados() + " tratamientos", false),
                caja("Total del plan", Formato.soles(t.total()), "", false),
                avance);
        fila.setAlignment(Pos.CENTER_LEFT);
        totalesPlan.getChildren().setAll(fila);
    }

    private Node caja(String rotulo, String valor, String detalle, boolean acento) {
        Label v = Ui.etiqueta(valor, "total-valor");
        if (acento) {
            v.getStyleClass().add("acento");
        }
        VBox c = new VBox(2, Ui.etiqueta(rotulo.toUpperCase(), "campo-rotulo"), v,
                Ui.etiqueta(detalle, "texto-tenue", "texto-pequeno"));
        c.getStyleClass().add("total-caja");
        c.setMinWidth(170);
        return c;
    }

    /** Diálogo para agregar un tratamiento del catálogo a una pieza / cara. */
    private void dialogoAgregarTratamiento(Integer fdi, Superficie cara) {
        ComboBox<Tratamiento> trat = new ComboBox<>();
        trat.getItems().setAll(catalogo.activos());
        trat.setMaxWidth(Double.MAX_VALUE);
        trat.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Tratamiento t, boolean vacio) {
                super.updateItem(t, vacio);
                setText(vacio || t == null ? null : t.getNombre() + "  —  " + Formato.soles(t.getPrecio()));
            }
        });
        trat.setButtonCell(trat.getCellFactory().call(null));

        ComboBox<Integer> pieza = new ComboBox<>();
        // Se escribe el número ("16") o el nombre ("molar") y la lista se filtra.
        com.dentavista.ui.Mascaras.buscable(pieza, Pieza.TODAS, p -> p + " · " + Pieza.nombre(p));
        pieza.setValue(fdi);
        pieza.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Superficie> superficie = new ComboBox<>();
        superficie.getItems().setAll(Superficie.CARAS);
        superficie.setValue(cara);
        superficie.setMaxWidth(Double.MAX_VALUE);
        superficie.setConverter(new StringConverter<>() {
            @Override
            public String toString(Superficie s) {
                return s == null ? "" : s.etiqueta(pieza.getValue() == null ? 11 : pieza.getValue());
            }

            @Override
            public Superficie fromString(String s) {
                return null;
            }
        });
        Label ayuda = Ui.etiqueta("", "texto-tenue", "texto-pequeno");
        ayuda.setWrapText(true);
        Runnable actualizar = () -> {
            Tratamiento t = trat.getValue();
            boolean requierePieza = t == null || t.isRequierePieza();
            boolean porCara = t != null && t.isPorCara();
            pieza.setDisable(!requierePieza);
            superficie.setDisable(!porCara);
            ayuda.setText(t == null ? "" : (t.getDescripcion() == null ? "" : t.getDescripcion())
                    + (porCara ? " · Se cobra por cara." : requierePieza ? " · Se aplica a la pieza completa." : " · Procedimiento general."));
        };
        trat.valueProperty().addListener((o, a, b) -> actualizar.run());
        actualizar.run();

        VBox contenido = new VBox(12, Ui.campo("Tratamiento", trat), Ui.campo("Pieza", pieza),
                Ui.campo("Cara", superficie), ayuda);
        notificador.dialogo(Feather.PLUS_CIRCLE, "Agregar tratamiento al plan", contenido, "Agregar", false, () -> {
            ItemPlan i = planes.agregar(paciente, trat.getValue(), pieza.getValue(), superficie.getValue());
            notificador.exito(i.getTratamiento().getNombre() + " agregado al plan (" + Formato.soles(i.getPrecio()) + ").");
            refrescarPlan();
        }, 520);
    }

    // ================================================================ historial

    private Node construirHistorial() {
        ScrollPane s = new ScrollPane(listaHistorial);
        s.setFitToWidth(true);
        listaHistorial.setPadding(new Insets(18, 24, 24, 24));
        return s;
    }

    private void refrescarHistorial() {
        listaHistorial.getChildren().clear();
        List<Consulta> todas = clinica.consultas(paciente);
        List<ItemPlan> realizadosTodos = planes.items(paciente).stream()
                .filter(i -> i.getEstado() == EstadoItem.REALIZADO && i.getConsulta() != null).toList();
        // Filtro por pieza: solo las consultas en las que se registró o trató esa pieza.
        List<Consulta> consultas = filtroPieza == null ? todas : todas.stream()
                .filter(c -> clinica.hallazgosDe(c).stream().anyMatch(h -> h.getPieza() == filtroPieza)
                        || realizadosTodos.stream().anyMatch(i -> filtroPieza.equals(i.getPieza())
                        && i.getConsulta().getId().equals(c.getId())))
                .toList();
        Label t = Ui.etiqueta(filtroPieza == null ? "Historia clínica" : "Historia de la pieza " + filtroPieza,
                "titulo-pagina");
        Label s = Ui.etiqueta(filtroPieza == null
                ? todas.size() + " consultas desde " + (todas.isEmpty() ? "—"
                : Formato.fecha(todas.get(todas.size() - 1).getFecha()))
                : Pieza.nombre(filtroPieza) + " · " + consultas.size() + " de " + todas.size() + " consultas",
                "subtitulo-pagina");

        ComboBox<Integer> pieza = new ComboBox<>();
        java.util.TreeSet<Integer> conRegistros = new java.util.TreeSet<>(
                java.util.Comparator.comparingInt((Integer f) -> Pieza.TODAS.indexOf(f)));
        clinica.hallazgos(paciente).forEach(h -> conRegistros.add(h.getPieza()));
        planes.items(paciente).stream().map(ItemPlan::getPieza).filter(java.util.Objects::nonNull)
                .forEach(conRegistros::add);
        pieza.getItems().add(null);
        pieza.getItems().addAll(conRegistros);
        pieza.setValue(filtroPieza);
        pieza.setPrefWidth(300);
        pieza.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer f) {
                return f == null ? "Todas las piezas" : f + " · " + Pieza.nombre(f);
            }

            @Override
            public Integer fromString(String x) {
                return null;
            }
        });
        pieza.setOnAction(e -> {
            filtroPieza = pieza.getValue();
            refrescarHistorial();
        });
        Ui.tooltip(pieza, "Mostrar solo las consultas de una pieza");
        Button detalle = Ui.secundario("Historial de la pieza", Feather.CLOCK);
        detalle.setDisable(filtroPieza == null);
        detalle.setOnAction(e -> dialogoHistorialPieza.abrir(paciente, filtroPieza));
        HBox filtro = new HBox(8, Ui.icono(Feather.FILTER, 15), pieza, detalle);
        filtro.setAlignment(Pos.CENTER_LEFT);

        HBox cab = new HBox(12, new VBox(2, t, s), Ui.espaciador(), filtro);
        cab.setAlignment(Pos.CENTER_LEFT);
        cab.setPadding(new Insets(0, 0, 14, 0));
        listaHistorial.getChildren().add(cab);
        if (consultas.isEmpty()) {
            listaHistorial.getChildren().add(Ui.vacio(Feather.CLOCK, "Sin consultas",
                    "La primera consulta se abre automáticamente al registrar un hallazgo."));
            return;
        }
        recetasDelPaciente = filtroPieza == null ? recetas.recetas(paciente) : List.of();
        List<ItemPlan> realizados = filtroPieza == null ? realizadosTodos
                : realizadosTodos.stream().filter(i -> filtroPieza.equals(i.getPieza())).toList();
        for (int k = 0; k < consultas.size(); k++) {
            Consulta c = consultas.get(k);
            listaHistorial.getChildren().add(hito(c, realizados, k == consultas.size() - 1));
        }
    }

    private Node hito(Consulta c, List<ItemPlan> realizados, boolean ultimo) {
        Region punto = new Region();
        punto.getStyleClass().add("hito-punto");
        Region lin = new Region();
        lin.getStyleClass().add("hito-linea");
        VBox.setVgrow(lin, Priority.ALWAYS);
        lin.setVisible(!ultimo);
        VBox eje = new VBox(4, punto, lin);
        eje.setAlignment(Pos.TOP_CENTER);
        eje.setPadding(new Insets(20, 0, 0, 0));
        eje.setMinWidth(14);

        Label fecha = Ui.etiqueta(Formato.fechaLarga(c.getFecha()), "tarjeta-titulo");
        Label motivo = Ui.etiqueta(c.getMotivo() + (c.getOdontologo() != null ? "  ·  " + c.getOdontologo().getNombre() : ""),
                "texto-tenue");
        Button ver = Ui.secundario("Ver odontograma en esta fecha", Feather.BOX);
        ver.setOnAction(e -> {
            tabOdontograma.fire();
            int idx = cronologia.indexOf(cronologia.stream().filter(x -> x.getId().equals(c.getId())).findFirst().orElse(null));
            if (idx >= 0) {
                linea.setValue(idx);
            }
        });
        HBox cab = new HBox(10, new VBox(2, fecha, motivo), Ui.espaciador(), ver);
        cab.setAlignment(Pos.CENTER_LEFT);

        FlowPane chips = new FlowPane(6, 6);
        for (Hallazgo h : clinica.hallazgosDe(c)) {
            if (filtroPieza != null && h.getPieza() != filtroPieza) {
                continue;
            }
            String texto = h.getPieza() + (h.getSuperficie().esCara() ? " " + h.getSuperficie().sigla(h.getPieza()) : "")
                    + " · " + h.getEstado().getEtiqueta();
            Label chip = new Label(texto, Ui.muestra(h.getEstado().getColor()));
            chip.getStyleClass().addAll("chip", "neutro");
            chip.setGraphicTextGap(6);
            chips.getChildren().add(chip);
        }
        VBox tarjeta = Ui.tarjeta(cab);
        if (!chips.getChildren().isEmpty()) {
            tarjeta.getChildren().addAll(Ui.etiqueta("HALLAZGOS Y REGISTROS", "campo-rotulo"), chips);
        }
        List<com.dentavista.modelo.Receta> recetasConsulta = recetasDelPaciente.stream()
                .filter(r -> r.getConsulta() != null && r.getConsulta().getId().equals(c.getId())).toList();
        if (!recetasConsulta.isEmpty()) {
            VBox rs = new VBox(4);
            for (com.dentavista.modelo.Receta r : recetasConsulta) {
                String meds = r.getItems().stream().map(com.dentavista.modelo.Receta.Item::getMedicamento)
                        .reduce((a, b) -> a + ", " + b).orElse("");
                Button imprimir = Ui.botonIcono(Feather.PRINTER, "Volver a imprimir la receta");
                imprimir.setOnAction(e -> notificador.intentar(() -> abrirArchivo(pdf.receta(r))));
                Label l = Ui.etiqueta(meds, "");
                l.setWrapText(true);
                HBox f = new HBox(8, Ui.icono(Feather.FILE_PLUS, 14), l, Ui.espaciador(), imprimir);
                f.setAlignment(Pos.CENTER_LEFT);
                rs.getChildren().add(f);
            }
            tarjeta.getChildren().addAll(Ui.etiqueta("RECETAS", "campo-rotulo"), rs);
        }
        List<ItemPlan> deEsta = realizados.stream().filter(i -> i.getConsulta().getId().equals(c.getId())).toList();
        if (!deEsta.isEmpty()) {
            VBox trats = new VBox(4);
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (ItemPlan i : deEsta) {
                total = total.add(i.getPrecio());
                HBox f = new HBox(8, Ui.icono(Feather.CHECK_CIRCLE, 14),
                        Ui.etiqueta(i.getTratamiento().getNombre() + (i.getPieza() != null ? " · " + i.ubicacion() : "")),
                        Ui.espaciador(), Ui.etiqueta(Formato.soles(i.getPrecio()), "texto-fuerte"));
                f.setAlignment(Pos.CENTER_LEFT);
                trats.getChildren().add(f);
            }
            HBox tot = new HBox(Ui.espaciador(), Ui.etiqueta("Total de la consulta: " + Formato.soles(total), "texto-fuerte"));
            tarjeta.getChildren().addAll(Ui.etiqueta("TRATAMIENTOS REALIZADOS", "campo-rotulo"), trats, tot);
        }
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        HBox fila = new HBox(14, eje, tarjeta);
        fila.setPadding(new Insets(0, 0, 14, 0));
        return fila;
    }

    // ================================================================= acceso

    /** Crea o renueva el usuario con el que el paciente entra a ver su odontograma y pedir citas. */
    private void dialogoAcceso() {
        var actual = usuarios.accesoDe(paciente);
        javafx.scene.control.TextField login = new javafx.scene.control.TextField(
                actual.map(com.dentavista.modelo.Usuario::getUsuario).orElse(paciente.getDni()));
        Label info = Ui.etiqueta(actual.isPresent()
                ? "Este paciente ya tiene acceso. Se generará una contraseña temporal nueva (la anterior deja de funcionar)."
                : "Con este acceso el paciente verá su odontograma 3D y su presupuesto, y podrá pedir o reprogramar citas.",
                "texto-tenue");
        info.setWrapText(true);
        Label clave = Ui.etiqueta("DentaVista genera una contraseña temporal para entregársela; al ingresar por primera "
                + "vez el paciente creará la suya.", "texto-tenue", "texto-pequeno");
        clave.setWrapText(true);
        clave.setMinHeight(Region.USE_PREF_SIZE);
        clave.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clave, Priority.ALWAYS);
        HBox nota = new HBox(10, Ui.icono(Feather.LOCK, 16), clave);
        nota.setAlignment(Pos.CENTER_LEFT);
        nota.getStyleClass().add("asistente-opcion");
        VBox c = new VBox(12, info, Ui.campo("Usuario (por defecto, su DNI)", login), nota);
        notificador.dialogo(Feather.KEY, "Acceso del paciente", c,
                actual.isPresent() ? "Generar nueva contraseña" : "Crear acceso", false, () -> {
                    String temporal = com.dentavista.servicio.Seguridad.claveTemporal();
                    var u = usuarios.crearAccesoPaciente(paciente, login.getText(), temporal, true);
                    com.dentavista.ui.Credenciales.mostrarDespues(notificador, "Acceso del paciente listo",
                            paciente.nombreCompleto(), u.getUsuario(), temporal, datosClinica.nombre());
                }, 480);
    }

    // ===================================================================== PDF

    private void exportarInforme() {
        notificador.intentar(() -> {
            Odontograma hoy = clinica.odontograma(paciente);
            PdfService.Imagenes img = new PdfService.Imagenes(
                    Imagenes.aPng(Capturas.vista3D(hoy, VisorDental3D.Vista.FRONTAL, 1100, 700)),
                    Imagenes.aPng(Capturas.vista3D(hoy, VisorDental3D.Vista.SUPERIOR, 760, 520)),
                    Imagenes.aPng(Capturas.vista3D(hoy, VisorDental3D.Vista.INFERIOR, 760, 520)),
                    Imagenes.aPng(Capturas.vista2D(hoy, 1400, 400)));
            entregar(pdf.informe(paciente, hoy, clinica.consultasCronologicas(paciente), img), "Informe clínico");
        });
    }

    private void exportarPresupuesto() {
        notificador.intentar(() -> entregar(pdf.presupuesto(paciente, planes.items(paciente), planes.totales(paciente),
                pagos.cuenta(paciente)), "Presupuesto"));
    }

    private void entregar(Path archivo, String que) {
        notificador.exito(que + " guardado en " + archivo.getParent() + ".");
        abrirArchivo(archivo);
    }

    private static <T> TableColumn<ItemPlan, T> col(String titulo, Function<ItemPlan, T> valor) {
        TableColumn<ItemPlan, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleObjectProperty<>(valor.apply(d.getValue())));
        c.setSortable(false);
        return c;
    }
}
