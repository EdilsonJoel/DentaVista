package com.erejm.dentavista.vista;

import com.erejm.dentavista.modelo.Cita;
import com.erejm.dentavista.modelo.EstadoCita;
import com.erejm.dentavista.modelo.Usuario;
import com.erejm.dentavista.servicio.CitaService;
import com.erejm.dentavista.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Agenda semanal del consultorio (lunes a sábado). Clic en un espacio libre para agendar,
 * clic en una cita para confirmarla, atenderla o cancelarla.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class AgendaVista {

    private static final LocalTime INICIO = LocalTime.of(8, 0);
    private static final LocalTime FIN = LocalTime.of(19, 0);
    private static final double ALTO_BLOQUE = 30;
    private static final Locale PE = Locale.forLanguageTag("es-PE");
    private static final String[] COLORES_DOCTOR = {"#0d9488", "#4f46e5", "#db2777", "#ea580c"};

    private final CitaService citas;
    private final DialogoCita dialogo;
    private final DetalleCita detalle;

    private LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
    private Usuario filtro;
    private List<Usuario> odontologos;
    private final Label lblSemana = new Label();
    private final Label pendientes = new Label();
    private final GridPane grilla = new GridPane();

    public Node vista() {
        odontologos = citas.odontologos();
        if (LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
            lunes = lunes.plusWeeks(1);
        }
        Label titulo = Ui.etiqueta("Agenda", "titulo-pagina");
        lblSemana.getStyleClass().add("subtitulo-pagina");
        Button ant = Ui.botonIcono(Feather.CHEVRON_LEFT, "Semana anterior");
        ant.setOnAction(e -> mover(-1));
        Button sig = Ui.botonIcono(Feather.CHEVRON_RIGHT, "Semana siguiente");
        sig.setOnAction(e -> mover(1));
        Button hoy = Ui.secundario("Hoy", null);
        hoy.setOnAction(e -> {
            lunes = LocalDate.now().with(DayOfWeek.MONDAY);
            dibujar();
        });
        ComboBox<Usuario> doctor = new ComboBox<>();
        doctor.getItems().add(null);
        doctor.getItems().addAll(odontologos);
        doctor.setConverter(new StringConverter<>() {
            @Override
            public String toString(Usuario u) {
                return u == null ? "Todos los odontólogos" : u.getNombre();
            }

            @Override
            public Usuario fromString(String s) {
                return null;
            }
        });
        doctor.setValue(null);
        doctor.valueProperty().addListener((o, a, b) -> {
            filtro = b;
            dibujar();
        });
        Button nueva = Ui.primario("Nueva cita", Feather.PLUS);
        nueva.setOnAction(e -> dialogo.abrir(new DialogoCita.Sugerencia(null, filtro, null, null), c -> {
            lunes = c.getInicio().toLocalDate().with(DayOfWeek.MONDAY);
            dibujar();
        }));
        HBox nav = new HBox(6, ant, hoy, sig);
        nav.setAlignment(Pos.CENTER_LEFT);
        HBox cab = new HBox(12, new VBox(2, titulo, lblSemana), Ui.espaciador(), pendientes, nav, doctor, nueva);
        cab.setAlignment(Pos.CENTER_LEFT);

        HBox leyenda = new HBox(16);
        for (int i = 0; i < odontologos.size(); i++) {
            leyenda.getChildren().add(new Label(odontologos.get(i).getNombre(), Ui.muestra(color(odontologos.get(i)))));
        }
        leyenda.getChildren().addAll(new Label("Por confirmar", Ui.muestra("#f59e0b")),
                Ui.etiqueta("Clic en un espacio libre para agendar", "texto-tenue", "texto-pequeno"));
        leyenda.getChildren().forEach(n -> ((Label) n).setGraphicTextGap(6));
        leyenda.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(grilla);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox tarjeta = Ui.tarjeta(leyenda, scroll);
        tarjeta.setPadding(new Insets(14));
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        VBox pagina = new VBox(16, cab, tarjeta);
        pagina.getStyleClass().add("pagina");
        dibujar();
        return pagina;
    }

    private void mover(int semanas) {
        lunes = lunes.plusWeeks(semanas);
        dibujar();
    }

    private void dibujar() {
        LocalDate sabado = lunes.plusDays(5);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("d 'de' MMMM", PE);
        lblSemana.setText("Semana del " + f.format(lunes) + " al " + f.format(sabado) + " de " + sabado.getYear());
        long porConfirmar = citas.porConfirmar();
        pendientes.setText(porConfirmar + (porConfirmar == 1 ? " solicitud por confirmar" : " solicitudes por confirmar"));
        pendientes.getStyleClass().setAll("chip", porConfirmar > 0 ? "alerta" : "neutro");
        pendientes.setGraphic(Ui.icono(Feather.BELL, 12));

        List<Cita> semana = citas.entre(lunes, sabado).stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA)
                .filter(c -> filtro == null || c.getOdontologo().getId().equals(filtro.getId()))
                .toList();

        grilla.getChildren().clear();
        grilla.getColumnConstraints().clear();
        ColumnConstraints horas = new ColumnConstraints(54);
        grilla.getColumnConstraints().add(horas);
        int bloques = (int) (Duration.between(INICIO, FIN).toMinutes() / CitaService.MINUTOS_BLOQUE);
        double alto = bloques * ALTO_BLOQUE;

        // Columna de horas.
        Pane colHoras = new Pane();
        colHoras.setPrefHeight(alto);
        for (int b = 0; b <= bloques; b += 2) {
            Label l = Ui.etiqueta(INICIO.plusMinutes((long) b * CitaService.MINUTOS_BLOQUE).toString(), "texto-tenue", "texto-pequeno");
            l.setLayoutY(b * ALTO_BLOQUE - 8);
            l.setLayoutX(6);
            colHoras.getChildren().add(l);
        }
        grilla.add(new Region(), 0, 0);
        grilla.add(colHoras, 0, 1);

        for (int d = 0; d < 6; d++) {
            LocalDate dia = lunes.plusDays(d);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(130);
            grilla.getColumnConstraints().add(cc);
            boolean esHoy = dia.equals(LocalDate.now());
            Label nombre = new Label(dia.getDayOfWeek().getDisplayName(TextStyle.FULL, PE) + " " + dia.getDayOfMonth());
            nombre.getStyleClass().add(esHoy ? "agenda-dia-hoy" : "agenda-dia");
            long n = semana.stream().filter(c -> c.getInicio().toLocalDate().equals(dia)).count();
            Label cuenta = Ui.etiqueta(n == 0 ? "" : n + (n == 1 ? " cita" : " citas"), "texto-tenue", "texto-pequeno");
            VBox cabDia = new VBox(0, nombre, cuenta);
            cabDia.setAlignment(Pos.CENTER);
            cabDia.setPadding(new Insets(0, 0, 8, 0));
            grilla.add(cabDia, d + 1, 0);
            grilla.add(columnaDia(dia, alto, bloques, semana), d + 1, 1);
        }
    }

    private Pane columnaDia(LocalDate dia, double alto, int bloques, List<Cita> semana) {
        Pane col = new Pane();
        col.setPrefHeight(alto);
        col.setMinHeight(alto);
        col.getStyleClass().add(dia.equals(LocalDate.now()) ? "agenda-columna-hoy" : "agenda-columna");

        // Bloques de fondo: horario de atención o cerrado.
        for (int b = 0; b < bloques; b++) {
            LocalDateTime t = dia.atTime(INICIO.plusMinutes((long) b * CitaService.MINUTOS_BLOQUE));
            Region r = new Region();
            boolean abierto = CitaService.dentroDelHorario(t, CitaService.MINUTOS_BLOQUE);
            boolean pasado = t.isBefore(LocalDateTime.now());
            r.getStyleClass().add(!abierto ? "agenda-cerrado" : pasado ? "agenda-pasado" : "agenda-libre");
            r.setLayoutY(b * ALTO_BLOQUE);
            r.setPrefHeight(ALTO_BLOQUE);
            r.prefWidthProperty().bind(col.widthProperty());
            if (abierto && !pasado) {
                r.setCursor(Cursor.HAND);
                r.setOnMouseClicked(e -> dialogo.abrir(new DialogoCita.Sugerencia(null, filtro, t, null), c -> dibujar()));
            }
            col.getChildren().add(r);
        }
        // Marca de "ahora".
        if (dia.equals(LocalDate.now())) {
            double y = Duration.between(INICIO, LocalTime.now()).toMinutes() / (double) CitaService.MINUTOS_BLOQUE * ALTO_BLOQUE;
            if (y > 0 && y < alto) {
                Region ahora = new Region();
                ahora.getStyleClass().add("agenda-ahora");
                ahora.setLayoutY(y);
                ahora.prefWidthProperty().bind(col.widthProperty());
                col.getChildren().add(ahora);
            }
        }
        // Citas.
        for (Cita c : semana) {
            if (!c.getInicio().toLocalDate().equals(dia)) {
                continue;
            }
            double y = Duration.between(INICIO, c.getInicio().toLocalTime()).toMinutes() / (double) CitaService.MINUTOS_BLOQUE * ALTO_BLOQUE;
            double h = c.getDuracion() / (double) CitaService.MINUTOS_BLOQUE * ALTO_BLOQUE - 3;
            col.getChildren().add(bloqueCita(c, y + 1, h, col));
        }
        return col;
    }

    private Node bloqueCita(Cita c, double y, double h, Pane col) {
        Label hora = new Label(c.getInicio().toLocalTime() + " · " + c.getPaciente().nombreCompleto());
        hora.getStyleClass().add("agenda-cita-titulo");
        Label desc = new Label(c.descripcion());
        desc.getStyleClass().add("agenda-cita-texto");
        VBox b = new VBox(1, hora);
        if (h > 34) {
            b.getChildren().add(desc);
        }
        b.getStyleClass().add("agenda-cita");
        String color = color(c.getOdontologo());
        String fondo = switch (c.getEstado()) {
            case SOLICITADA -> "rgba(245,158,11,0.16)";
            case ATENDIDA -> "derive(" + color + ", 70%)";
            case NO_ASISTIO -> "rgba(100,116,139,0.18)";
            default -> "derive(" + color + ", 80%)";
        };
        b.setStyle("-fx-background-color: " + fondo + "; -fx-border-color: "
                + (c.getEstado() == EstadoCita.SOLICITADA ? "#f59e0b" : color) + ";"
                + (c.getEstado() == EstadoCita.SOLICITADA ? " -fx-border-style: segments(4, 3);" : "")
                + (c.getEstado() == EstadoCita.NO_ASISTIO || c.getEstado() == EstadoCita.ATENDIDA ? " -fx-opacity: 0.75;" : ""));
        b.setLayoutY(y);
        b.setLayoutX(3);
        b.setPrefHeight(h);
        b.setMinHeight(h);
        b.setMaxHeight(h);
        b.prefWidthProperty().bind(col.widthProperty().subtract(6));
        Ui.tooltip(b, c.getPaciente().nombreCompleto() + "\n" + c.getInicio().toLocalTime() + " – " + c.fin().toLocalTime()
                + " · " + c.getOdontologo().getNombre() + "\n" + c.descripcion() + "\n" + c.getEstado().getEtiqueta());
        b.setOnMouseClicked(e -> {
            detalle.abrir(c, this::dibujar);
            e.consume();
        });
        return b;
    }

    private String color(Usuario u) {
        int i = odontologos.indexOf(u);
        return COLORES_DOCTOR[Math.floorMod(i < 0 ? u.getId().intValue() : i, COLORES_DOCTOR.length)];
    }

    /** Formato corto para otras vistas. */
    static String horaCorta(Cita c) {
        return c.getInicio().toLocalTime() + " – " + c.fin().toLocalTime();
    }
}
