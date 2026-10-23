package com.erejm.dentavista.vista;

import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.servicio.PacienteService;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Ui;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Diálogo de alta / edición de paciente (lo usan la lista y la ficha).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
final class FormularioPaciente {

    private FormularioPaciente() {
    }

    static void abrir(Notificador notificador, PacienteService servicio, Paciente existente, Consumer<Paciente> alGuardar) {
        Paciente p = existente != null ? existente : new Paciente();

        TextField dni = new TextField(p.getDni());
        Ui.soloDigitos(dni, 8);
        dni.setPromptText("8 dígitos");
        TextField nombres = new TextField(p.getNombres());
        TextField apellidos = new TextField(p.getApellidos());
        DatePicker nacimiento = new DatePicker(p.getFechaNacimiento());
        com.erejm.dentavista.ui.Mascaras.fecha(nacimiento);
        com.erejm.dentavista.ui.Mascaras.nombrePropio(nombres);
        com.erejm.dentavista.ui.Mascaras.nombrePropio(apellidos);
        ComboBox<String> sexo = new ComboBox<>();
        sexo.getItems().addAll("Femenino", "Masculino", "Otro");
        sexo.setValue(p.getSexo() == null ? null : switch (p.getSexo()) {
            case "F" -> "Femenino";
            case "M" -> "Masculino";
            default -> "Otro";
        });
        TextField telefono = new TextField(p.getTelefono());
        TextField email = new TextField(p.getEmail());
        com.erejm.dentavista.ui.Mascaras.celular(telefono);
        com.erejm.dentavista.ui.Mascaras.correo(email);
        TextField direccion = new TextField(p.getDireccion());
        TextField alergias = new TextField(p.getAlergias());
        alergias.setPromptText("Penicilina, látex, anestésicos… (vacío si no tiene)");
        TextArea antecedentes = new TextArea(p.getAntecedentes());
        antecedentes.setPromptText("Diabetes, hipertensión, anticoagulantes, embarazo…");
        antecedentes.setPrefRowCount(2);
        antecedentes.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(12);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        c.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c, c);
        g.add(Ui.campo("DNI *", dni), 0, 0);
        g.add(Ui.campo("Fecha de nacimiento", nacimiento), 1, 0);
        g.add(Ui.campo("Nombres *", nombres), 0, 1);
        g.add(Ui.campo("Apellidos *", apellidos), 1, 1);
        g.add(Ui.campo("Sexo", sexo), 0, 2);
        g.add(Ui.campo("Teléfono", telefono), 1, 2);
        g.add(Ui.campo("Correo", email), 0, 3);
        g.add(Ui.campo("Dirección", direccion), 1, 3);
        VBox alerta = Ui.campo("Alergias", alergias);
        g.add(alerta, 0, 4, 2, 1);
        g.add(Ui.campo("Antecedentes médicos", antecedentes), 0, 5, 2, 1);

        notificador.dialogo(existente == null ? Feather.USER_PLUS : Feather.EDIT_2,
                existente == null ? "Nuevo paciente" : "Editar paciente", g,
                existente == null ? "Registrar paciente" : "Guardar cambios", false, () -> {
                    p.setDni(dni.getText());
                    p.setNombres(nombres.getText());
                    p.setApellidos(apellidos.getText());
                    LocalDate f = nacimiento.getValue();
                    p.setFechaNacimiento(f);
                    p.setSexo(sexo.getValue() == null ? null : sexo.getValue().substring(0, 1));
                    p.setTelefono(vacioANull(telefono.getText()));
                    p.setEmail(vacioANull(email.getText()));
                    p.setDireccion(vacioANull(direccion.getText()));
                    p.setAlergias(vacioANull(alergias.getText()));
                    p.setAntecedentes(vacioANull(antecedentes.getText()));
                    Paciente guardado = servicio.guardar(p);
                    notificador.exito(existente == null
                            ? "Paciente " + guardado.nombreCompleto() + " registrado."
                            : "Datos de " + guardado.nombreCompleto() + " actualizados.");
                    alGuardar.accept(guardado);
                }, 620);
        dni.requestFocus();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
