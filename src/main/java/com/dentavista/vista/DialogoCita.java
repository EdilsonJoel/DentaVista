package com.dentavista.vista;

import com.dentavista.modelo.Cita;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Usuario;
import com.dentavista.servicio.CitaService;
import com.dentavista.servicio.NegocioException;
import com.dentavista.servicio.PacienteService;
import com.dentavista.servicio.PlanService;
import com.dentavista.servicio.SesionService;
import com.dentavista.ui.Formato;
import com.dentavista.ui.Notificador;
import com.dentavista.ui.Ui;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo para agendar (personal) o solicitar (paciente) una cita. Muestra solo los
 * horarios libres del odontólogo elegido y permite ligar la cita a un tratamiento
 * pendiente del plan.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class DialogoCita {

    /** Valores iniciales (cualquiera puede ser null). */
    public record Sugerencia(Paciente paciente, Usuario odontologo, LocalDateTime inicio, ItemPlan item) {
    }

    private final CitaService citas;
    private final PacienteService pacientes;
    private final PlanService planes;
    private final SesionService sesion;
    private final Notificador notificador;

    public void abrir(Sugerencia s, Consumer<Cita> alGuardar) {
        mostrar(s, null, alGuardar);
    }

    /** Mismo formulario, con la cita actual precargada: se elige nuevo día, hora u odontólogo. */
    public void reprogramar(Cita cita, Consumer<Cita> alGuardar) {
        mostrar(new Sugerencia(cita.getPaciente(), cita.getOdontologo(), cita.getInicio(), cita.getItemPlan()), cita, alGuardar);
    }

    private void mostrar(Sugerencia s, Cita existente, Consumer<Cita> alGuardar) {
        boolean esPaciente = sesion.esPaciente();
        boolean reprogramando = existente != null;
        Paciente fijo = esPaciente ? sesion.pacientePropio() : s.paciente();

        ComboBox<Paciente> paciente = new ComboBox<>();
        paciente.setMaxWidth(Double.MAX_VALUE);
        if (fijo != null) {
            paciente.getItems().setAll(fijo);
            paciente.setValue(fijo);
            paciente.setDisable(true);
        } else {
            // Se busca escribiendo parte del nombre o del DNI.
            com.dentavista.ui.Mascaras.buscable(paciente, pacientes.buscar(""), p -> p.nombreCompleto() + " · DNI " + p.getDni());
        }
        if (fijo != null) {
            paciente.setConverter(conv(p -> p.nombreCompleto() + " · DNI " + p.getDni(), "Elige un paciente"));
        }
        paciente.setPromptText("Escribe nombre o DNI del paciente");

        ComboBox<Usuario> odontologo = new ComboBox<>();
        odontologo.getItems().setAll(citas.odontologos());
        odontologo.setMaxWidth(Double.MAX_VALUE);
        odontologo.setConverter(conv(Usuario::getNombre, "Elige un odontólogo"));
        odontologo.setValue(s.odontologo() != null ? s.odontologo()
                : odontologo.getItems().isEmpty() ? null : odontologo.getItems().get(0));

        DatePicker fecha = new DatePicker(s.inicio() != null ? s.inicio().toLocalDate() : siguienteDiaHabil());
        com.dentavista.ui.Mascaras.fecha(fecha);
        fecha.setMaxWidth(Double.MAX_VALUE);
        fecha.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean vacio) {
                super.updateItem(item, vacio);
                setDisable(vacio || item.isBefore(LocalDate.now()) || item.getDayOfWeek() == DayOfWeek.SUNDAY);
            }
        });

        ToggleGroup gDur = new ToggleGroup();
        HBox duraciones = new HBox(4);
        for (int m : new int[]{30, 60, 90}) {
            ToggleButton b = new ToggleButton(m + " min");
            b.setUserData(m);
            b.setToggleGroup(gDur);
            b.getStyleClass().add(m == 30 ? "left-pill" : m == 90 ? "right-pill" : "center-pill");
            if (m == (reprogramando ? existente.getDuracion() : 30)) {
                b.setSelected(true);
            }
            duraciones.getChildren().add(b);
        }

        ComboBox<ItemPlan> tratamiento = new ComboBox<>();
        tratamiento.setMaxWidth(Double.MAX_VALUE);
        tratamiento.setPromptText("Consulta o control (sin tratamiento)");
        tratamiento.setConverter(conv(i -> i.getTratamiento().getNombre() + (i.getPieza() != null ? " · " + i.ubicacion() : "")
                + " · " + Formato.soles(i.getPrecio()), "Consulta o control (sin tratamiento)"));
        Runnable cargarTratamientos = () -> {
            Paciente p = paciente.getValue();
            tratamiento.getItems().clear();
            tratamiento.getItems().add(null);
            if (p != null) {
                planes.items(p).stream().filter(i -> i.getEstado() == EstadoItem.PENDIENTE).forEach(tratamiento.getItems()::add);
            }
            tratamiento.setValue(s.item());
        };
        cargarTratamientos.run();
        paciente.valueProperty().addListener((o, a, b) -> cargarTratamientos.run());

        TextField motivo = new TextField(reprogramando ? existente.getMotivo() : null);
        motivo.setPromptText(esPaciente ? "Cuéntanos qué necesitas (opcional)" : "Motivo (opcional)");

        FlowPane horas = new FlowPane(6, 6);
        ToggleGroup gHora = new ToggleGroup();
        Label sinHoras = Ui.etiqueta("", "texto-tenue", "texto-pequeno");
        Runnable cargarHoras = () -> {
            horas.getChildren().clear();
            Usuario od = odontologo.getValue();
            LocalDate dia = fecha.getValue();
            int dur = gDur.getSelectedToggle() == null ? 30 : (int) gDur.getSelectedToggle().getUserData();
            if (od == null || dia == null) {
                sinHoras.setText("Elige odontólogo y fecha.");
                return;
            }
            List<LocalTime> libres = citas.horariosLibres(od, dia, dur, existente);
            sinHoras.setText(libres.isEmpty()
                    ? "No hay horarios libres ese día. Prueba con otra fecha u odontólogo."
                    : libres.size() + " horarios libres · " + Formato.fechaLarga(dia.atStartOfDay()));
            for (LocalTime h : libres) {
                ToggleButton b = new ToggleButton(h.toString());
                b.setUserData(h);
                b.setToggleGroup(gHora);
                b.getStyleClass().add("hora-libre");
                if (s.inicio() != null && s.inicio().toLocalDate().equals(dia) && s.inicio().toLocalTime().equals(h)) {
                    b.setSelected(true);
                }
                horas.getChildren().add(b);
            }
        };
        cargarHoras.run();
        odontologo.valueProperty().addListener((o, a, b) -> cargarHoras.run());
        fecha.valueProperty().addListener((o, a, b) -> cargarHoras.run());
        gDur.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                a.setSelected(true);
                return;
            }
            cargarHoras.run();
        });

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        g.add(Ui.campo("Paciente", paciente), 0, 0, 2, 1);
        g.add(Ui.campo("Odontólogo", odontologo), 0, 1);
        g.add(Ui.campo("Fecha", fecha), 1, 1);
        g.add(Ui.campo("Duración", duraciones), 0, 2, 2, 1);
        VBox bloqueHoras = new VBox(6, horas, sinHoras);
        g.add(Ui.campo("Horario libre", bloqueHoras), 0, 3, 2, 1);
        if (!reprogramando) {
            g.add(Ui.campo("Tratamiento del plan", tratamiento), 0, 4, 2, 1);
            g.add(Ui.campo("Motivo", motivo), 0, 5, 2, 1);
        }
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, c);
        VBox contenido = new VBox(10, g);
        if (reprogramando) {
            contenido.getChildren().add(0, Ui.chip("Cita actual: " + Formato.fechaHora(existente.getInicio()) + " con "
                    + existente.getOdontologo().getNombre() + " · " + existente.descripcion(), "info", Feather.CALENDAR));
        }
        if (esPaciente) {
            contenido.getChildren().add(Ui.chip("Tu solicitud quedará \"por confirmar\" hasta que el consultorio la confirme.",
                    "info", Feather.INFO));
        }

        String titulo = reprogramando ? "Reprogramar cita" : esPaciente ? "Pedir una cita" : "Agendar cita";
        String accion = reprogramando ? "Reprogramar" : esPaciente ? "Solicitar cita" : "Agendar";
        notificador.dialogo(Feather.CALENDAR, titulo, contenido, accion, false, () -> {
                    if (gHora.getSelectedToggle() == null) {
                        throw new NegocioException("Elige uno de los horarios libres.");
                    }
                    LocalTime h = (LocalTime) gHora.getSelectedToggle().getUserData();
                    int dur = (int) gDur.getSelectedToggle().getUserData();
                    if (reprogramando) {
                        Cita cita = citas.reprogramar(existente, odontologo.getValue(), fecha.getValue().atTime(h), dur);
                        notificador.exito("Cita reprogramada para el " + Formato.fechaHora(cita.getInicio())
                                + (esPaciente ? ". Queda por confirmar." : " con " + cita.getOdontologo().getNombre() + "."));
                        alGuardar.accept(cita);
                        return;
                    }
                    Cita cita = citas.agendar(paciente.getValue(), odontologo.getValue(), fecha.getValue().atTime(h), dur,
                            motivo.getText(), tratamiento.getValue());
                    notificador.exito((esPaciente ? "Solicitud enviada: " : "Cita agendada: ")
                            + Formato.fechaHora(cita.getInicio()) + " con " + cita.getOdontologo().getNombre() + ".");
                    alGuardar.accept(cita);
                }, 600);
    }

    private static LocalDate siguienteDiaHabil() {
        LocalDate d = LocalDate.now().plusDays(1);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    private static <T> StringConverter<T> conv(java.util.function.Function<T, String> f, String vacio) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? vacio : f.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }
}
