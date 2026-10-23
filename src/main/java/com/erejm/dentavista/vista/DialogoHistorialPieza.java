package com.erejm.dentavista.vista;

import com.erejm.dentavista.JavaFxApplication;
import com.erejm.dentavista.dominio.Odontograma;
import com.erejm.dentavista.modelo.Hallazgo;
import com.erejm.dentavista.modelo.ItemPlan;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.servicio.ClinicaService;
import com.erejm.dentavista.servicio.HistorialPiezaService;
import com.erejm.dentavista.servicio.PdfService;
import com.erejm.dentavista.ui.Capturas;
import com.erejm.dentavista.ui.Formato;
import com.erejm.dentavista.ui.Imagenes;
import com.erejm.dentavista.ui.Notificador;
import com.erejm.dentavista.ui.Odontograma2D;
import com.erejm.dentavista.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de una sola pieza: acercamiento 3D, resumen (estado, tratamientos, importes)
 * y la evolución consulta por consulta con el diagrama de sus caras tras cada una.
 * Se exporta a PDF para entregarlo al paciente o adjuntarlo a una interconsulta.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class DialogoHistorialPieza {

    private final HistorialPiezaService historiales;
    private final ClinicaService clinica;
    private final PdfService pdf;
    private final Notificador notificador;

    public void abrir(Paciente paciente, int fdi) {
        HistorialPiezaService.Historial h = historiales.de(paciente, fdi);
        Odontograma hoy = clinica.odontograma(paciente);

        // ---- cabecera
        Label numero = Ui.etiqueta(String.valueOf(fdi), "pieza-numero");
        numero.setMinWidth(Region.USE_PREF_SIZE);
        Label nombre = Ui.etiqueta(h.nombre(), "dialogo-titulo");
        Label sub = Ui.etiqueta("Historial de la pieza · " + paciente.nombreCompleto(), "texto-tenue");
        Button exportar = Ui.primario("Exportar PDF", Feather.DOWNLOAD);
        exportar.setOnAction(e -> exportar(h, hoy));
        Button cerrar = Ui.botonIcono(Feather.X, "Cerrar");
        cerrar.setCancelButton(true);
        cerrar.setOnAction(e -> notificador.cerrar());
        HBox cab = new HBox(14, numero, new VBox(2, nombre, sub), Ui.espaciador(), exportar, cerrar);
        cab.setAlignment(Pos.CENTER_LEFT);

        // ---- resumen: 3D + datos
        ImageView foto = new ImageView(Capturas.pieza3D(hoy, fdi, 360, 250));
        foto.setFitWidth(300);
        foto.setPreserveRatio(true);
        StackPane marcoFoto = new StackPane(foto);
        marcoFoto.getStyleClass().add("historial-pieza-foto");
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 300 * 250 / 360.0);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        foto.setClip(clip);

        Label estado = estadoChip(h.actual());
        VBox datos = new VBox(12,
                new VBox(4, Ui.etiqueta("ESTADO ACTUAL", "campo-rotulo"), estado,
                        Ui.etiqueta(h.actual().sana() ? "Sin hallazgos" : h.actual().descripcion(), "texto-tenue")),
                indicadores(h));
        HBox.setHgrow(datos, Priority.ALWAYS);
        HBox resumen = new HBox(18, marcoFoto, datos);
        resumen.setAlignment(Pos.CENTER_LEFT);

        // ---- evolución (lo más reciente arriba)
        VBox linea = new VBox(0);
        List<HistorialPiezaService.Evento> eventos = new ArrayList<>(h.eventos());
        java.util.Collections.reverse(eventos);
        for (int i = 0; i < eventos.size(); i++) {
            linea.getChildren().add(evento(fdi, eventos.get(i), i == eventos.size() - 1));
        }
        if (eventos.isEmpty()) {
            linea.getChildren().add(Ui.vacio(Feather.CHECK_CIRCLE, "Sin registros",
                    "Esta pieza está sana: no se le registró ningún hallazgo ni tratamiento."));
        }
        VBox cuerpo = new VBox(18, resumen, Ui.etiqueta("EVOLUCIÓN", "campo-rotulo"), linea);
        if (!h.pendientes().isEmpty()) {
            VBox pend = new VBox(6);
            for (ItemPlan i : h.pendientes()) {
                HBox f = new HBox(10, Ui.icono(Feather.CLOCK, 14), Ui.etiqueta(i.getTratamiento().getNombre()
                        + (i.getSuperficie() != null && i.getSuperficie().esCara() ? " · " + i.getSuperficie().etiqueta(fdi) : ""),
                        "texto-fuerte"), Ui.espaciador(), Ui.etiqueta(Formato.soles(i.getPrecio()), "texto-fuerte"));
                f.setAlignment(Pos.CENTER_LEFT);
                f.getStyleClass().add("historial-item");
                pend.getChildren().add(f);
            }
            cuerpo.getChildren().addAll(Ui.etiqueta("POR REALIZAR EN ESTA PIEZA", "campo-rotulo"), pend);
        }
        cuerpo.setPadding(new Insets(4, 6, 4, 0));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(560);
        scroll.setPrefViewportHeight(540);
        scroll.getStyleClass().add("edge-to-edge");

        VBox caja = new VBox(16, cab, scroll);
        caja.getStyleClass().add("dialogo");
        caja.setMaxSize(820, Region.USE_PREF_SIZE);
        caja.setPrefWidth(820);
        notificador.mostrar(caja);
    }

    private Node indicadores(HistorialPiezaService.Historial h) {
        HBox fila = new HBox(10,
                indicador("Consultas", String.valueOf(h.eventos().size()),
                        h.primeraAtencion() == null ? "—" : "desde " + Formato.fecha(h.primeraAtencion())),
                indicador("Realizado", Formato.soles(h.realizado()), h.tratamientosRealizados() + " tratamientos"),
                indicador("Por realizar", Formato.soles(h.porRealizar()), h.pendientes().size() + " tratamientos"));
        return fila;
    }

    private static Node indicador(String rotulo, String valor, String detalle) {
        VBox v = new VBox(2, Ui.etiqueta(rotulo.toUpperCase(), "campo-rotulo"), Ui.etiqueta(valor, "total-valor"),
                Ui.etiqueta(detalle, "texto-tenue", "texto-pequeno"));
        v.getStyleClass().add("total-caja");
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    /** Un punto de la línea de tiempo: fecha, diagrama de cómo quedó, hallazgos y tratamientos. */
    private Node evento(int fdi, HistorialPiezaService.Evento e, boolean ultimo) {
        Region punto = new Region();
        punto.getStyleClass().add("hito-punto");
        Region lin = new Region();
        lin.getStyleClass().add("hito-linea");
        VBox.setVgrow(lin, Priority.ALWAYS);
        lin.setVisible(!ultimo);
        VBox eje = new VBox(4, punto, lin);
        eje.setAlignment(Pos.TOP_CENTER);
        eje.setPadding(new Insets(16, 0, 0, 0));
        eje.setMinWidth(14);

        StackPane diagrama = new StackPane(Odontograma2D.diagramaPieza(fdi, e.estadoDespues(), 70, null, (p, c) -> { }));
        diagrama.getStyleClass().add("historial-pieza-diagrama");
        diagrama.setMinSize(96, 96);
        Ui.tooltip(diagrama, "Cómo quedó la pieza al terminar esta consulta");

        Label fecha = Ui.etiqueta(Formato.fechaLarga(e.fecha()), "tarjeta-titulo");
        String detalle = e.consulta() == null ? "" : (e.consulta().getMotivo() == null ? "" : e.consulta().getMotivo())
                + (e.consulta().getOdontologo() != null ? "  ·  " + e.consulta().getOdontologo().getNombre() : "");
        VBox textos = new VBox(6, fecha);
        if (!detalle.isBlank()) {
            textos.getChildren().add(Ui.etiqueta(detalle, "texto-tenue"));
        }
        FlowPane chips = new FlowPane(6, 6);
        for (Hallazgo h : e.hallazgos()) {
            Label c = new Label(h.getEstado().getEtiqueta() + " · "
                    + (h.getSuperficie().esCara() ? h.getSuperficie().etiqueta(fdi) : "pieza completa"),
                    Ui.muestra(h.getEstado().getColor()));
            c.getStyleClass().add("chip-hallazgo");
            c.setGraphicTextGap(6);
            chips.getChildren().add(c);
        }
        if (!chips.getChildren().isEmpty()) {
            textos.getChildren().add(chips);
        }
        for (Hallazgo h : e.hallazgos()) {
            if (h.getNota() != null && !h.getNota().isBlank()) {
                Label n = Ui.etiqueta("“" + h.getNota() + "”", "texto-pequeno");
                n.setWrapText(true);
                textos.getChildren().add(n);
            }
        }
        for (ItemPlan r : e.realizados()) {
            HBox t = new HBox(8, Ui.icono(Feather.CHECK_CIRCLE, 14), Ui.etiqueta(r.getTratamiento().getNombre(), "texto-fuerte"),
                    Ui.espaciador(), Ui.etiqueta(Formato.soles(r.getPrecio()), "texto-fuerte"));
            t.setAlignment(Pos.CENTER_LEFT);
            textos.getChildren().add(t);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox tarjeta = new HBox(16, diagrama, textos);
        tarjeta.getStyleClass().add("historial-pieza-evento");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        HBox fila = new HBox(12, eje, tarjeta);
        fila.setPadding(new Insets(0, 0, 10, 0));
        return fila;
    }

    private static Label estadoChip(Odontograma.EstadoPieza e) {
        if (e.sana()) {
            return Ui.chip("Sana", "exito", Feather.CHECK);
        }
        if (e.ausente()) {
            return Ui.chip("Ausente", "neutro", Feather.MINUS_CIRCLE);
        }
        boolean patologia = (e.general() != null && e.general().esPatologia())
                || e.caras().values().stream().anyMatch(com.erejm.dentavista.dominio.EstadoDental::esPatologia);
        return patologia ? Ui.chip("Requiere tratamiento", "peligro", Feather.ALERT_CIRCLE)
                : Ui.chip("Tratada", "info", Feather.SHIELD);
    }

    private void exportar(HistorialPiezaService.Historial h, Odontograma hoy) {
        notificador.intentar(() -> {
            byte[] foto = Imagenes.aPng(Capturas.pieza3D(hoy, h.fdi(), 720, 500));
            List<byte[]> diagramas = h.eventos().stream()
                    .map(e -> Imagenes.aPng(Capturas.diagrama(h.fdi(), e.estadoDespues(), 90))).toList();
            Path archivo = pdf.historialPieza(h, foto, diagramas);
            notificador.exito("Historial de la pieza " + h.fdi() + " guardado en " + archivo.getParent() + ".");
            JavaFxApplication.get().hostServices().showDocument(archivo.toUri().toString());
        });
    }
}
