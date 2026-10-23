package com.erejm.dentavista.vista;

import com.erejm.dentavista.JavaFxApplication;
import com.erejm.dentavista.dominio.Medicamento;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Receta;
import com.erejm.dentavista.servicio.NegocioException;
import com.erejm.dentavista.servicio.PdfService;
import com.erejm.dentavista.servicio.RecetaService;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo de receta: medicamentos del vademécum (o escritos a mano) con su indicación
 * habitual precargada. Cruza cada medicamento con las alergias y antecedentes del
 * paciente y exige confirmar las alertas antes de emitir.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class DialogoReceta {

    private final RecetaService recetas;
    private final PdfService pdf;
    private final Notificador notificador;

    /** Una fila editable de la receta. */
    private record Fila(ComboBox<String> medicamento, TextField indicacion, HBox nodo) {
    }

    public void abrir(Paciente p, Runnable alEmitir) {
        VBox filas = new VBox(8);
        List<Fila> lista = new ArrayList<>();
        Label alertas = new Label();
        alertas.getStyleClass().add("dialogo-error");
        alertas.setWrapText(true);
        alertas.setMaxWidth(Double.MAX_VALUE);
        alertas.setVisible(false);
        alertas.setManaged(false);
        CheckBox confirmo = new CheckBox("Revisé las alertas y emito la receta bajo mi criterio clínico");
        confirmo.setWrapText(true);
        confirmo.setVisible(false);
        confirmo.setManaged(false);

        Runnable revisar = () -> {
            List<String> a = recetas.alertas(p, items(lista));
            alertas.setText(String.join("\n", a));
            boolean hay = !a.isEmpty();
            alertas.setVisible(hay);
            alertas.setManaged(hay);
            confirmo.setVisible(hay);
            confirmo.setManaged(hay);
            if (!hay) {
                confirmo.setSelected(false);
            }
        };

        Runnable agregar = () -> {
            ComboBox<String> med = new ComboBox<>();
            med.setEditable(true);
            for (Medicamento m : Medicamento.values()) {
                med.getItems().add(m.getNombre());
            }
            med.setPromptText("Medicamento");
            med.setPrefWidth(250);
            TextField ind = new TextField();
            ind.setPromptText("Dosis, frecuencia y días");
            HBox.setHgrow(ind, Priority.ALWAYS);
            Button quitar = Ui.botonIcono(Feather.X, "Quitar");
            HBox nodo = new HBox(8, med, ind, quitar);
            nodo.setAlignment(Pos.CENTER_LEFT);
            Fila f = new Fila(med, ind, nodo);
            med.valueProperty().addListener((o, a, nombre) -> {
                Medicamento.porNombre(nombre).ifPresent(m -> {
                    if (ind.getText() == null || ind.getText().isBlank()) {
                        ind.setText(m.getIndicacion());
                    }
                });
                revisar.run();
            });
            med.getEditor().focusedProperty().addListener((o, a, foco) -> {
                if (!foco) {
                    med.setValue(med.getEditor().getText());
                }
            });
            quitar.setOnAction(e -> {
                lista.remove(f);
                filas.getChildren().remove(nodo);
                revisar.run();
            });
            lista.add(f);
            filas.getChildren().add(nodo);
        };
        agregar.run();
        Button otra = Ui.secundario("Agregar medicamento", Feather.PLUS);
        otra.setOnAction(e -> agregar.run());

        TextArea indicaciones = new TextArea();
        indicaciones.setPromptText("Indicaciones generales: dieta blanda, no fumar, aplicar hielo las primeras 24 horas…");
        indicaciones.setPrefRowCount(2);
        indicaciones.setWrapText(true);

        VBox cont = new VBox(12);
        if (p.getAlergias() != null) {
            cont.getChildren().add(Ui.chip("Alergia: " + p.getAlergias(), "peligro", Feather.ALERT_OCTAGON));
        }
        if (p.getAntecedentes() != null) {
            cont.getChildren().add(Ui.chip(p.getAntecedentes(), "alerta", Feather.HEART));
        }
        cont.getChildren().addAll(Ui.etiqueta("Rp.", "tarjeta-titulo"), filas, otra, Ui.campo("Indicaciones", indicaciones),
                alertas, confirmo);

        notificador.dialogo(Feather.FILE_PLUS, "Receta para " + p.nombreCompleto(), cont, "Emitir receta", false, () -> {
            lista.forEach(f -> {
                if (f.medicamento().getEditor() != null) {
                    f.medicamento().setValue(f.medicamento().getEditor().getText());
                }
            });
            revisar.run();
            if (alertas.isVisible() && !confirmo.isSelected()) {
                throw new NegocioException("Hay alertas de alergia o contraindicación: revísalas y marca la confirmación, "
                        + "o cambia el medicamento.");
            }
            Receta r = recetas.emitir(p, items(lista), indicaciones.getText(), confirmo.isSelected());
            notificador.exito("Receta emitida con " + r.getItems().size() + (r.getItems().size() == 1 ? " medicamento." : " medicamentos."));
            alEmitir.run();
            JavaFxApplication.get().hostServices().showDocument(pdf.receta(r).toUri().toString());
        }, 680);
    }

    private static List<Receta.Item> items(List<Fila> filas) {
        List<Receta.Item> r = new ArrayList<>();
        for (Fila f : filas) {
            String m = f.medicamento().getValue();
            if (m == null || m.isBlank()) {
                m = f.medicamento().getEditor() == null ? null : f.medicamento().getEditor().getText();
            }
            if (m != null && !m.isBlank()) {
                r.add(new Receta.Item(m, f.indicacion().getText()));
            }
        }
        return r;
    }
}
