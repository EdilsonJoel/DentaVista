package com.erejm.dentavista.vista;

import com.erejm.dentavista.modelo.Cita;
import com.erejm.dentavista.modelo.Consulta;
import com.erejm.dentavista.modelo.EstadoCita;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.servicio.CitaService;
import com.erejm.dentavista.servicio.EstadisticaService;
import com.erejm.dentavista.servicio.PacienteService;
import com.erejm.dentavista.servicio.PagoService;
import com.erejm.dentavista.servicio.SesionService;
import com.erejm.dentavista.ui.Formato;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Inicio: lo que el consultorio necesita ver al empezar el día. Citas de hoy,
 * solicitudes por confirmar, pacientes que deben y pacientes con tratamientos
 * pendientes que todavía no tienen cita.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class InicioVista {

    private final EstadisticaService estadisticas;
    private final SesionService sesion;
    private final PacienteService pacientes;
    private final Notificador notificador;
    private final Navegador navegador;
    private final CitaService citas;
    private final DetalleCita detalle;
    private final DialogoCita dialogoCita;
    private final PagoService pagos;

    public Node vista() {
        EstadisticaService.Resumen r = estadisticas.resumen();

        String nombre = sesion.getUsuario().getNombre();
        Label saludo = Ui.etiqueta(saludo() + ", " + primerNombre(nombre), "titulo-pagina");
        Label fecha = Ui.etiqueta(Formato.fechaLarga(LocalDate.now().atStartOfDay()), "subtitulo-pagina");
        Button cita = Ui.secundario("Nueva cita", Feather.CALENDAR);
        cita.setOnAction(e -> dialogoCita.abrir(new DialogoCita.Sugerencia(null, null, null, null),
                c -> navegador.irA(Navegador.Seccion.INICIO)));
        Button nuevo = Ui.primario("Nuevo paciente", Feather.USER_PLUS);
        nuevo.setOnAction(e -> FormularioPaciente.abrir(notificador, pacientes, null, navegador::abrirFicha));
        HBox cab = new HBox(10, new VBox(2, saludo, fecha), Ui.espaciador(), cita, nuevo);
        cab.setAlignment(Pos.CENTER_LEFT);

        List<Cita> hoy = citas.delDia(LocalDate.now()).stream().filter(c -> c.getEstado() != EstadoCita.CANCELADA).toList();
        long pendientesHoy = hoy.stream().filter(c -> c.getEstado().abierta()).count();

        GridPane kpis = new GridPane();
        kpis.setHgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            kpis.getColumnConstraints().add(c);
        }
        kpis.add(kpi(Feather.CALENDAR, "tono-turquesa", String.valueOf(hoy.size()),
                "Citas hoy · " + pendientesHoy + " por atender"), 0, 0);
        kpis.add(kpi(Feather.USERS, "tono-azul", String.valueOf(r.pacientes()),
                "Pacientes activos · " + r.consultasMes() + " consultas este mes"), 1, 0);
        kpis.add(kpi(Feather.DOLLAR_SIGN, "tono-violeta", Formato.soles(pagos.cobradoEsteMes()),
                "Cobrado este mes · " + pagos.pagosEsteMes() + " pagos"), 2, 0);
        int alta = pacientes.paraDarDeAlta().size();
        Node kpiAlta = kpi(Feather.CHECK_CIRCLE, "tono-ambar", String.valueOf(alta),
                "Para dar de alta · terminaron y pagaron todo");
        kpiAlta.getStyleClass().add("kpi-enlace");
        kpiAlta.setOnMouseClicked(e -> navegador.irA(Navegador.Seccion.PACIENTES));
        kpis.add(kpiAlta, 3, 0);

        // Columna izquierda: el día.
        VBox izquierda = new VBox(16, tarjetaCitasHoy(hoy), tarjetaPorAgendar());
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        // Columna derecha: pendientes de gestión.
        VBox derecha = new VBox(16);
        List<Cita> solicitudes = citas.solicitudes();
        if (!solicitudes.isEmpty()) {
            derecha.getChildren().add(tarjetaSolicitudes(solicitudes));
        }
        derecha.getChildren().addAll(tarjetaDeudores(), tarjetaConsultas());
        derecha.setMinWidth(380);
        derecha.setPrefWidth(420);
        derecha.setMaxWidth(460);

        VBox pagina = new VBox(20, cab, kpis, new HBox(16, izquierda, derecha));
        pagina.getStyleClass().add("pagina");
        ScrollPane scroll = new ScrollPane(pagina);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // ---------------------------------------------------------------- tarjetas

    private VBox tarjetaCitasHoy(List<Cita> hoy) {
        VBox lista = new VBox(6);
        for (Cita c : hoy) {
            Label hora = Ui.etiqueta(c.getInicio().toLocalTime().toString(), "texto-fuerte");
            hora.setMinWidth(46);
            Label n = Ui.etiqueta(c.getPaciente().nombreCompleto(), "texto-fuerte");
            Label d = Ui.etiqueta(c.descripcion() + " · " + c.getOdontologo().getNombre(), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            textos.setMinWidth(0);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Label est = Ui.chip(c.getEstado().getEtiqueta(), DetalleCita.claseEstado(c.getEstado()), null);
            est.setMinWidth(Region.USE_PREF_SIZE);
            HBox f = new HBox(12, hora, textos, est);
            if (c.getPaciente().getAlergias() != null) {
                Label al = Ui.chip("Alergia", "peligro", Feather.ALERT_OCTAGON);
                al.setMinWidth(Region.USE_PREF_SIZE);
                Ui.tooltip(al, "Alergia: " + c.getPaciente().getAlergias());
                f.getChildren().add(2, al);
            }
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            f.setOnMouseClicked(e -> detalle.abrir(c, () -> navegador.irA(Navegador.Seccion.INICIO)));
            lista.getChildren().add(f);
        }
        if (hoy.isEmpty()) {
            lista.getChildren().add(Ui.vacio(Feather.CALENDAR, "Sin citas para hoy", "Agenda una cita desde aquí o desde la agenda."));
        }
        Button agenda = Ui.secundario("Ver agenda", Feather.CALENDAR);
        agenda.setOnAction(e -> navegador.irA(Navegador.Seccion.AGENDA));
        return Ui.tarjeta(Ui.cabecera("Citas de hoy", "Clic en una cita para atenderla, confirmarla o enviar el recordatorio", agenda),
                lista);
    }

    private VBox tarjetaPorAgendar() {
        VBox lista = new VBox(6);
        List<CitaService.PorAgendar> porAgendar = citas.pacientesPorAgendar();
        for (CitaService.PorAgendar a : porAgendar.stream().limit(6).toList()) {
            Paciente p = a.paciente();
            Label n = Ui.etiqueta(p.nombreCompleto(), "texto-fuerte");
            Label d = Ui.etiqueta(a.tratamientos() + (a.tratamientos() == 1 ? " tratamiento pendiente · " : " tratamientos pendientes · ")
                    + Formato.soles(a.monto()) + (p.getTelefono() != null ? " · " + p.getTelefono() : ""), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button agendar = Ui.secundario("Agendar", Feather.CALENDAR);
            agendar.setMinWidth(Region.USE_PREF_SIZE);
            agendar.setOnAction(e -> dialogoCita.abrir(new DialogoCita.Sugerencia(p, null, null, null),
                    c -> navegador.irA(Navegador.Seccion.INICIO)));
            Button ficha = Ui.botonIcono(Feather.ARROW_RIGHT, "Abrir ficha");
            ficha.setOnAction(e -> navegador.abrirFicha(p));
            HBox f = new HBox(10, Ui.avatar(p.iniciales(), p.getDni(), 32), textos, agendar, ficha);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            lista.getChildren().add(f);
        }
        if (porAgendar.isEmpty()) {
            lista.getChildren().add(Ui.etiqueta("Todos los pacientes con tratamientos pendientes ya tienen cita.", "texto-tenue"));
        }
        return Ui.tarjeta(Ui.cabecera("Tratamientos sin cita",
                porAgendar.size() + " pacientes con tratamientos pendientes y sin cita próxima: conviene llamarlos"), lista);
    }

    private VBox tarjetaSolicitudes(List<Cita> solicitudes) {
        VBox lista = new VBox(6);
        for (Cita c : solicitudes) {
            Label n = Ui.etiqueta(c.getPaciente().nombreCompleto(), "texto-fuerte");
            Label d = Ui.etiqueta(Formato.fechaHora(c.getInicio()) + " · " + c.getOdontologo().getNombre(), "texto-tenue", "texto-pequeno");
            VBox textos = new VBox(1, n, d);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button ok = Ui.botonIcono(Feather.CHECK, "Confirmar cita");
            ok.setOnAction(e -> {
                if (notificador.intentar(() -> citas.confirmar(c))) {
                    notificador.exito("Cita de " + c.getPaciente().nombreCompleto() + " confirmada.");
                    navegador.irA(Navegador.Seccion.INICIO);
                }
            });
            Button ver = Ui.botonIcono(Feather.EYE, "Ver detalle");
            ver.setOnAction(e -> detalle.abrir(c, () -> navegador.irA(Navegador.Seccion.INICIO)));
            HBox f = new HBox(8, textos, ver, ok);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            lista.getChildren().add(f);
        }
        return Ui.tarjeta(Ui.cabecera("Solicitudes por confirmar", "Pedidas por los pacientes desde su acceso"), lista);
    }

    private VBox tarjetaDeudores() {
        VBox lista = new VBox(4);
        List<PagoService.Deudor> deudores = pagos.deudores();
        for (PagoService.Deudor d : deudores.stream().limit(5).toList()) {
            Label n = Ui.etiqueta(d.paciente().nombreCompleto(), "texto-fuerte");
            Label m = Ui.etiqueta(Formato.soles(d.deuda()), "texto-fuerte");
            m.setStyle("-fx-text-fill: #dc2626;");
            m.setMinWidth(Region.USE_PREF_SIZE);
            HBox f = new HBox(10, Ui.avatar(d.paciente().iniciales(), d.paciente().getDni(), 30), n, Ui.espaciador(), m);
            f.setAlignment(Pos.CENTER_LEFT);
            f.getStyleClass().add("cita-tarjeta");
            f.setOnMouseClicked(e -> navegador.abrirFicha(d.paciente()));
            lista.getChildren().add(f);
        }
        if (deudores.isEmpty()) {
            lista.getChildren().add(Ui.etiqueta("Nadie tiene saldo pendiente.", "texto-tenue"));
        }
        java.math.BigDecimal total = deudores.stream().map(PagoService.Deudor::deuda)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return Ui.tarjeta(Ui.cabecera("Por cobrar · " + Formato.soles(total),
                deudores.size() + " pacientes deben tratamientos ya realizados"), lista);
    }

    private VBox tarjetaConsultas() {
        VBox lista = new VBox(4);
        for (Consulta c : estadisticas.ultimasConsultas().stream().limit(5).toList()) {
            Paciente p = c.getPaciente();
            Label n = Ui.etiqueta(p.nombreCompleto(), "texto-fuerte");
            Label m = Ui.etiqueta(c.getMotivo(), "texto-tenue", "texto-pequeno");
            m.setMaxWidth(220);
            Label f = Ui.etiqueta(Formato.relativo(c.getFecha()), "texto-tenue", "texto-pequeno");
            f.setMinWidth(Region.USE_PREF_SIZE);
            HBox h = new HBox(10, Ui.avatar(p.iniciales(), p.getDni(), 30), new VBox(1, n, m), Ui.espaciador(), f);
            h.setAlignment(Pos.CENTER_LEFT);
            h.getStyleClass().add("cita-tarjeta");
            h.setOnMouseClicked(e -> navegador.abrirFicha(p));
            lista.getChildren().add(h);
        }
        return Ui.tarjeta(Ui.cabecera("Últimas consultas", null), lista);
    }

    private Node kpi(Ikon icono, String tono, String valor, String rotulo) {
        StackPane ic = new StackPane(Ui.icono(icono, 18));
        ic.getStyleClass().addAll("kpi-icono", tono);
        Label v = Ui.etiqueta(valor, "kpi-valor");
        Label r = Ui.etiqueta(rotulo, "kpi-rotulo");
        r.setWrapText(true);
        VBox caja = new VBox(8, ic, v, r);
        caja.getStyleClass().add("kpi");
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private static String saludo() {
        LocalTime t = LocalTime.now();
        return t.getHour() < 12 ? "Buenos días" : t.getHour() < 19 ? "Buenas tardes" : "Buenas noches";
    }

    /** "Dra. Lucía Paredes Quispe" -> "Dra. Lucía". */
    private static String primerNombre(String nombre) {
        String[] p = nombre.split(" ");
        if (p.length >= 2 && p[0].endsWith(".")) {
            return p[0] + " " + p[1];
        }
        return p[0];
    }
}
