package com.erejm.dentavista.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Máscaras de entrada: el usuario escribe solo lo necesario y el campo completa el
 * formato (barras de la fecha, espacios del celular, decimales del monto) y descarta
 * lo que no corresponde, tanto al teclear como al pegar.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Mascaras {

    // Estricto: el 31/02 se rechaza en vez de "corregirse" al 29/02.
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);

    private Mascaras() {
    }

    // ------------------------------------------------------------------ fecha

    /**
     * Fecha dd/mm/aaaa: se escriben solo los dígitos ("12051996") y las barras aparecen
     * solas. Con año de dos dígitos ("120596") se completa el siglo al salir del campo.
     */
    public static void fecha(DatePicker picker) {
        picker.setEditable(true);
        picker.setPromptText("dd/mm/aaaa");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : DMY.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                return interpretar(s);
            }
        });
        TextField editor = picker.getEditor();
        editor.setTextFormatter(new TextFormatter<String>(cambio -> {
            if (!cambio.isContentChange()) {
                return cambio;
            }
            String digitos = cambio.getControlNewText().replaceAll("\\D", "");
            if (digitos.length() > 8) {
                digitos = digitos.substring(0, 8);
            }
            String formateado = conBarras(digitos, cambio.getControlText().length() > cambio.getControlNewText().length());
            cambio.setRange(0, cambio.getControlText().length());
            cambio.setText(formateado);
            cambio.selectRange(formateado.length(), formateado.length());
            return cambio;
        }));
        // Al salir del campo se confirma la fecha (o se descarta si no es válida).
        editor.focusedProperty().addListener((o, a, foco) -> {
            if (!foco) {
                LocalDate d = interpretar(editor.getText());
                picker.setValue(d);
                editor.setText(d == null ? "" : DMY.format(d));
            }
        });
    }

    /** "12051996" -> "12/05/1996"; al borrar no se reinsertan barras colgando. */
    static String conBarras(String d, boolean borrando) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            if (i == 2 || i == 4) {
                sb.append('/');
            }
            sb.append(d.charAt(i));
        }
        if (!borrando && (d.length() == 2 || d.length() == 4)) {
            sb.append('/');
        }
        return sb.toString();
    }

    /** Interpreta dd/mm/aaaa o dd/mm/aa (siglo deducido). Devuelve null si no es válida. */
    public static LocalDate interpretar(String texto) {
        if (texto == null) {
            return null;
        }
        String d = texto.replaceAll("\\D", "");
        try {
            if (d.length() == 8) {
                return LocalDate.parse(d.substring(0, 2) + "/" + d.substring(2, 4) + "/" + d.substring(4), DMY);
            }
            if (d.length() == 6) {
                int yy = Integer.parseInt(d.substring(4));
                int actual = LocalDate.now().getYear() % 100;
                int anio = yy <= actual ? 2000 + yy : 1900 + yy;
                return LocalDate.parse(d.substring(0, 2) + "/" + d.substring(2, 4) + "/" + anio, DMY);
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }

    // ---------------------------------------------------------------- celular

    /** Celular peruano: solo dígitos, máximo 9, mostrado como "951 284 736". */
    public static void celular(TextInputControl campo) {
        campo.setPromptText("9## ### ###");
        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            if (!cambio.isContentChange()) {
                return cambio;
            }
            String digitos = cambio.getControlNewText().replaceAll("\\D", "");
            if (digitos.length() > 9) {
                digitos = digitos.substring(0, 9);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digitos.length(); i++) {
                if (i == 3 || i == 6) {
                    sb.append(' ');
                }
                sb.append(digitos.charAt(i));
            }
            cambio.setRange(0, cambio.getControlText().length());
            cambio.setText(sb.toString());
            cambio.selectRange(sb.length(), sb.length());
            return cambio;
        }));
    }

    // ------------------------------------------------------------------ montos

    /** Monto en soles: dígitos y un separador decimal (coma o punto), máximo 2 decimales. */
    public static void dinero(TextInputControl campo) {
        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            if (!cambio.isContentChange()) {
                return cambio;
            }
            String nuevo = cambio.getControlNewText().replace(',', '.');
            return nuevo.matches("\\d{0,7}(\\.\\d{0,2})?") ? normalizarComa(cambio) : null;
        }));
    }

    private static TextFormatter.Change normalizarComa(TextFormatter.Change c) {
        c.setText(c.getText().replace(',', '.'));
        return c;
    }

    /** Solo dígitos, con máximo. */
    public static void entero(TextInputControl campo, int maximo) {
        Ui.soloDigitos(campo, maximo);
    }

    // ----------------------------------------------------------------- textos

    /** Nombres propios: al salir del campo queda "Valeria Quispe Huanca". */
    public static void nombrePropio(TextInputControl campo) {
        campo.focusedProperty().addListener((o, a, foco) -> {
            if (!foco && campo.getText() != null) {
                campo.setText(capitalizar(campo.getText()));
            }
        });
    }

    static String capitalizar(String s) {
        String limpio = s.trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder();
        for (String p : limpio.split(" ")) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            String min = p.toLowerCase(Locale.forLanguageTag("es-PE"));
            // Conectores en minúscula: "de la Cruz", "del Pino".
            if (!sb.isEmpty() && List.of("de", "del", "la", "las", "los", "y").contains(min)) {
                sb.append(min);
            } else {
                sb.append(Character.toUpperCase(min.charAt(0))).append(min.substring(1));
            }
        }
        return sb.toString();
    }

    /** Correo: sin espacios y en minúsculas. */
    public static void correo(TextInputControl campo) {
        campo.setTextFormatter(new TextFormatter<String>(c -> {
            c.setText(c.getText().replace(" ", "").toLowerCase(Locale.ROOT));
            return c;
        }));
    }

    // ---------------------------------------------------------- autocompletar

    /**
     * Combo con búsqueda: se escribe parte del nombre (sin importar tildes ni
     * mayúsculas) o del DNI y la lista se filtra al instante.
     */
    public static <T> void buscable(ComboBox<T> combo, List<T> items, Function<T, String> texto) {
        ObservableList<T> base = FXCollections.observableArrayList(items);
        FilteredList<T> filtrados = new FilteredList<>(base, x -> true);
        combo.setItems(filtrados);
        combo.setEditable(true);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : texto.apply(t);
            }

            @Override
            public T fromString(String s) {
                return base.stream().filter(t -> texto.apply(t).equals(s)).findFirst().orElse(null);
            }
        });
        combo.getEditor().textProperty().addListener((o, anterior, escrito) -> {
            T sel = combo.getSelectionModel().getSelectedItem();
            if (sel != null && texto.apply(sel).equals(escrito)) {
                return; // el texto vino de elegir un elemento, no de escribir
            }
            String q = normalizar(escrito);
            filtrados.setPredicate(t -> q.isEmpty() || normalizar(texto.apply(t)).contains(q));
            if (combo.isFocused() || combo.getEditor().isFocused()) {
                if (!filtrados.isEmpty() && !combo.isShowing()) {
                    combo.show();
                }
            }
        });
        combo.getEditor().focusedProperty().addListener((o, a, foco) -> {
            if (!foco) {
                // Si lo escrito coincide con un único elemento, se elige solo.
                if (combo.getValue() == null && filtrados.size() == 1) {
                    combo.setValue(filtrados.get(0));
                }
            }
        });
    }

    private static String normalizar(String s) {
        return s == null ? "" : Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
    }
}
