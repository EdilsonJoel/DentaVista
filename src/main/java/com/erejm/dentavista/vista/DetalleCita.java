package com.erejm.dentavista.vista;

import com.erejm.dentavista.modelo.Cita;
import com.erejm.dentavista.modelo.EstadoCita;
import com.erejm.dentavista.servicio.CitaService;
import com.erejm.dentavista.servicio.SesionService;
import com.erejm.dentavista.ui.Formato;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

/**
 * Detalle de una cita con sus acciones (confirmar, atender, no asistió, cancelar).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class DetalleCita {

    private final CitaService citas;
    private final SesionService sesion;
    private final Notificador notificador;
    private final Navegador navegador;
    private final com.erejm.dentavista.servicio.DatosClinica clinica;
    private final DialogoCita dialogoCita;

    public void abrir(Cita c, Runnable alCambiar) {
        Label titulo = Ui.etiqueta(c.getPaciente().nombreCompleto(), "dialogo-titulo");
        Label estado = Ui.chip(c.getEstado().getEtiqueta(), claseEstado(c.getEstado()), null);
        HBox cab = new HBox(10, Ui.avatar(c.getPaciente().iniciales(), c.getPaciente().getDni(), 40),
                new VBox(2, titulo, Ui.etiqueta("DNI " + c.getPaciente().getDni()
                        + (c.getPaciente().getTelefono() != null ? " · " + c.getPaciente().getTelefono() : ""), "texto-tenue")),
                Ui.espaciador(), estado);
        cab.setAlignment(Pos.CENTER_LEFT);

        VBox datos = new VBox(8,
                fila(Feather.CALENDAR, Formato.fechaLarga(c.getInicio()) + " · " + c.getInicio().toLocalTime()
                        + " a " + c.fin().toLocalTime()),
                fila(Feather.USER, c.getOdontologo().getNombre()),
                fila(Feather.CLIPBOARD, c.descripcion()));
        if (c.getMotivo() != null && c.getItemPlan() != null) {
            datos.getChildren().add(fila(Feather.MESSAGE_SQUARE, c.getMotivo()));
        }
        if (c.getPaciente().getAlergias() != null) {
            datos.getChildren().add(Ui.chip("Alergia: " + c.getPaciente().getAlergias(), "peligro", Feather.ALERT_OCTAGON));
        }
        datos.getChildren().add(Ui.etiqueta("Registrada por " + c.getCreadaPor() + " el " + Formato.fechaHora(c.getCreadaEn()),
                "texto-tenue", "texto-pequeno"));

        FlowPane acciones = new FlowPane(8, 8);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        Button cerrar = Ui.secundario("Cerrar", Feather.X);
        cerrar.setCancelButton(true);
        cerrar.setOnAction(e -> notificador.cerrar());
        if (sesion.esPersonal()) {
            Button ficha = Ui.secundario("Abrir ficha", Feather.BOX);
            ficha.setOnAction(e -> {
                notificador.cerrar();
                navegador.abrirFicha(c.getPaciente());
            });
            acciones.getChildren().add(ficha);
        }
        if (c.getEstado().abierta() && sesion.esPersonal()) {
            Button whatsapp = Ui.secundario("Recordar por WhatsApp", Feather.MESSAGE_CIRCLE);
            String numero = numeroWhatsApp(c.getPaciente().getTelefono());
            whatsapp.setDisable(numero == null);
            Ui.tooltip(whatsapp, numero == null ? "El paciente no tiene un celular registrado"
                    : "Abre WhatsApp con el recordatorio listo para enviar a " + c.getPaciente().getTelefono());
            whatsapp.setOnAction(e -> com.erejm.dentavista.JavaFxApplication.get().hostServices().showDocument(
                    "https://wa.me/" + numero + "?text=" + java.net.URLEncoder.encode(mensaje(c),
                            java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20")));
            acciones.getChildren().add(whatsapp);
        }
        if (c.getEstado().abierta()) {
            if (sesion.esPersonal()) {
                Button noAsistio = Ui.secundario("No asistió", Feather.USER_X);
                noAsistio.setOnAction(e -> accion(() -> citas.marcarNoAsistio(c), "Marcada como no asistió.", alCambiar));
                acciones.getChildren().add(noAsistio);
            }
            Button cancelar = Ui.secundario("Cancelar cita", Feather.X_CIRCLE);
            cancelar.setOnAction(e -> accion(() -> citas.cancelar(c), "Cita cancelada: el horario quedó libre.", alCambiar));
            Button reprogramar = Ui.secundario("Reprogramar", Feather.REPEAT);
            Ui.tooltip(reprogramar, "Cambiar fecha, hora u odontólogo");
            reprogramar.setOnAction(e -> {
                notificador.cerrar();
                dialogoCita.reprogramar(c, x -> alCambiar.run());
            });
            acciones.getChildren().addAll(cancelar, reprogramar);
            if (sesion.esPersonal() && c.getEstado() == EstadoCita.SOLICITADA) {
                Button confirmar = Ui.primario("Confirmar", Feather.CHECK);
                confirmar.setOnAction(e -> accion(() -> citas.confirmar(c), "Cita confirmada.", alCambiar));
                acciones.getChildren().add(confirmar);
            } else if (sesion.esPersonal()) {
                Button atender = Ui.primario("Atender ahora", Feather.PLAY);
                atender.setOnAction(e -> {
                    if (notificador.intentar(() -> citas.marcarAtendida(c))) {
                        notificador.cerrar();
                        alCambiar.run();
                        navegador.abrirFicha(c.getPaciente());
                    }
                });
                acciones.getChildren().add(atender);
            }
        }
        acciones.getChildren().add(0, cerrar);

        VBox caja = new VBox(16, cab, datos, acciones);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(520, Region.USE_PREF_SIZE);
        caja.setPrefWidth(520);
        notificador.mostrar(caja);
    }

    /** Mensaje de recordatorio con fecha, hora, odontólogo y motivo. */
    String mensaje(Cita c) {
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(
                "EEEE d 'de' MMMM", java.util.Locale.forLanguageTag("es-PE"));
        return "Hola " + c.getPaciente().getNombres().split(" ")[0] + ", le saludamos de " + clinica.nombre()
                + ". Le recordamos su cita el " + f.format(c.getInicio()) + " a las " + c.getInicio().toLocalTime()
                + " con " + c.getOdontologo().getNombre() + " (" + c.descripcion() + "). "
                + "Por favor confirme su asistencia respondiendo este mensaje. ¡Gracias!";
    }

    /** Celular peruano en formato internacional para wa.me (51 + 9 dígitos), o null. */
    static String numeroWhatsApp(String telefono) {
        if (telefono == null) {
            return null;
        }
        String d = telefono.replaceAll("\\D", "");
        if (d.startsWith("51") && d.length() == 11) {
            d = d.substring(2);
        }
        return d.length() == 9 && d.startsWith("9") ? "51" + d : null;
    }

    private void accion(Runnable r, String mensaje, Runnable alCambiar) {
        if (notificador.intentar(r::run)) {
            notificador.cerrar();
            notificador.exito(mensaje);
            alCambiar.run();
        }
    }

    private static HBox fila(Feather icono, String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        HBox h = new HBox(10, Ui.icono(icono, 16), l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static String claseEstado(EstadoCita e) {
        return switch (e) {
            case SOLICITADA -> "alerta";
            case CONFIRMADA -> "info";
            case ATENDIDA -> "exito";
            case NO_ASISTIO -> "peligro";
            case CANCELADA -> "neutro";
        };
    }
}
