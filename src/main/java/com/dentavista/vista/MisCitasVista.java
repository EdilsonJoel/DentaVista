package com.dentavista.vista;

import com.dentavista.modelo.Cita;
import com.dentavista.modelo.Paciente;
import com.dentavista.servicio.CitaService;
import com.dentavista.servicio.DatosClinica;
import com.dentavista.servicio.PlanService;
import com.dentavista.servicio.SesionService;
import com.dentavista.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Portal del paciente: su próxima cita, pedir una nueva y su historial de citas.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MisCitasVista {

    private static final Locale PE = Locale.forLanguageTag("es-PE");

    private final CitaService citas;
    private final PlanService planes;
    private final SesionService sesion;
    private final DatosClinica clinica;
    private final DialogoCita dialogo;
    private final DetalleCita detalle;

    private final VBox contenido = new VBox(18);

    public Node vista() {
        contenido.getStyleClass().add("pagina");
        dibujar();
        ScrollPane s = new ScrollPane(contenido);
        s.setFitToWidth(true);
        return s;
    }

    private void dibujar() {
        Paciente p = sesion.pacientePropio();
        contenido.getChildren().clear();
        Label titulo = Ui.etiqueta("Mis citas", "titulo-pagina");
        Label sub = Ui.etiqueta(clinica.telefono().isEmpty() ? clinica.nombre() : clinica.nombre() + " · " + clinica.telefono(), "subtitulo-pagina");
        Button pedir = Ui.primario("Pedir una cita", Feather.CALENDAR);
        pedir.setOnAction(e -> dialogo.abrir(new DialogoCita.Sugerencia(p, null, null, null), c -> dibujar()));
        HBox cab = new HBox(12, new VBox(2, titulo, sub), Ui.espaciador(), pedir);
        cab.setAlignment(Pos.CENTER_LEFT);
        contenido.getChildren().add(cab);

        Optional<Cita> proxima = citas.proxima(p);
        if (proxima.isPresent()) {
            Cita c = proxima.get();
            DateTimeFormatter f = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", PE);
            Label cuando = new Label(capitalizar(f.format(c.getInicio())) + " · " + AgendaVista.horaCorta(c));
            cuando.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
            Label con = new Label(c.descripcion() + " con " + c.getOdontologo().getNombre());
            con.setStyle("-fx-font-size: 14px;");
            Label estado = new Label(c.getEstado() == com.dentavista.modelo.EstadoCita.SOLICITADA
                    ? "Solicitud enviada: el consultorio te la confirmará." : "Cita confirmada. Llega 10 minutos antes.");
            estado.setStyle("-fx-opacity: 0.85;");
            Button cambiar = new Button("Reprogramar", Ui.icono(Feather.REPEAT, 14));
            cambiar.getStyleClass().add("secundario");
            cambiar.setOnAction(e -> dialogo.reprogramar(c, x -> dibujar()));
            Button cancelar = new Button("Cancelar", Ui.icono(Feather.X, 14));
            cancelar.getStyleClass().add("secundario");
            cancelar.setOnAction(e -> detalle.abrir(c, this::dibujar));
            HBox fila = new HBox(12, new VBox(4, new Label("TU PRÓXIMA CITA"), cuando, con, estado), Ui.espaciador(),
                    new VBox(8, cambiar, cancelar));
            fila.setAlignment(Pos.CENTER_LEFT);
            VBox tarjeta = new VBox(fila);
            tarjeta.getStyleClass().add("proxima-cita");
            contenido.getChildren().add(tarjeta);
        } else {
            long pendientes = planes.totales(p).itemsPendientes();
            contenido.getChildren().add(Ui.tarjeta(Ui.vacio(Feather.CALENDAR, "No tienes citas programadas",
                    pendientes > 0 ? "Tienes " + pendientes + " tratamientos pendientes en tu plan. Pide una cita para continuar."
                            : "Pide una cita para tu control.")));
        }

        List<Cita> todas = citas.dePaciente(p);
        LocalDateTime ahora = LocalDateTime.now();
        VBox futuras = new VBox(8);
        VBox pasadas = new VBox(8);
        for (Cita c : todas) {
            (c.getInicio().isAfter(ahora) ? futuras : pasadas).getChildren().add(fila(c));
        }
        if (!futuras.getChildren().isEmpty()) {
            contenido.getChildren().add(Ui.tarjeta(Ui.cabecera("Próximas", null), futuras));
        }
        if (!pasadas.getChildren().isEmpty()) {
            contenido.getChildren().add(Ui.tarjeta(Ui.cabecera("Historial de citas", null), pasadas));
        }
    }

    private Node fila(Cita c) {
        Label dia = Ui.etiqueta(String.valueOf(c.getInicio().getDayOfMonth()), "dia");
        Label mes = Ui.etiqueta(c.getInicio().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, PE).toUpperCase()
                .replace(".", "") + " " + c.getInicio().getYear() % 100, "mes");
        VBox fecha = new VBox(0, dia, mes);
        fecha.getStyleClass().add("cita-fecha");
        Label t = Ui.etiqueta(c.descripcion(), "texto-fuerte");
        Label d = Ui.etiqueta(AgendaVista.horaCorta(c) + " · " + c.getOdontologo().getNombre(), "texto-tenue");
        VBox textos = new VBox(2, t, d);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox h = new HBox(14, fecha, textos, Ui.chip(c.getEstado().getEtiqueta(), DetalleCita.claseEstado(c.getEstado()), null));
        h.getStyleClass().add("cita-tarjeta");
        h.setOnMouseClicked(e -> detalle.abrir(c, this::dibujar));
        return h;
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
