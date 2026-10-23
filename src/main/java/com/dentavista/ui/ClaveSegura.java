package com.dentavista.ui;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Requisitos de la contraseña que se marcan mientras se escribe (8 caracteres, letras y
 * números, ambas iguales). Usado en el asistente de primer uso, en el primer ingreso con
 * contraseña temporal y en "Mi cuenta".
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class ClaveSegura {

    private static final String OK = "#16a34a";
    private static final String PENDIENTE = "#94a3b8";

    private ClaveSegura() {
    }

    public static VBox requisitos(PasswordField clave, PasswordField confirmacion) {
        Label largo = requisito("Al menos 8 caracteres");
        Label mezcla = requisito("Letras y números");
        Label iguales = requisito("Las dos contraseñas coinciden");
        ChangeListener<String> revisar = (o, a, b) -> {
            String c = clave.getText() == null ? "" : clave.getText();
            marcar(largo, c.length() >= 8);
            marcar(mezcla, c.chars().anyMatch(Character::isLetter) && c.chars().anyMatch(Character::isDigit));
            marcar(iguales, !c.isEmpty() && c.equals(confirmacion.getText()));
        };
        clave.textProperty().addListener(revisar);
        confirmacion.textProperty().addListener(revisar);
        revisar.changed(null, null, null);
        VBox v = new VBox(3, largo, mezcla, iguales);
        v.getStyleClass().add("clave-requisitos");
        return v;
    }

    private static Label requisito(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("texto-pequeno");
        l.setGraphicTextGap(8);
        return l;
    }

    private static void marcar(Label l, boolean cumple) {
        FontIcon ic = new FontIcon(cumple ? Feather.CHECK_CIRCLE : Feather.CIRCLE);
        ic.setIconSize(13);
        ic.setIconColor(javafx.scene.paint.Color.web(cumple ? OK : PENDIENTE));
        l.setGraphic(ic);
        l.setOpacity(cumple ? 1 : 0.75);
    }
}
