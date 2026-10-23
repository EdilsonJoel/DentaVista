package com.dentavista.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

/**
 * Tema claro / oscuro. La elección se recuerda entre sesiones.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
public class Tema {

    private static final String CLAVE = "tema-oscuro";
    private final Preferences prefs = Preferences.userNodeForPackage(Tema.class);
    private final BooleanProperty oscuro = new SimpleBooleanProperty(false);
    private Parent raiz;

    public void aplicarInicial() {
        oscuro.set(prefs.getBoolean(CLAVE, false));
        aplicar();
        oscuro.addListener((o, a, b) -> {
            prefs.putBoolean(CLAVE, b);
            aplicar();
        });
    }

    public void registrar(Parent raiz) {
        this.raiz = raiz;
        if (!raiz.getStyleClass().contains("app")) {
            raiz.getStyleClass().add("app");
        }
        aplicar();
    }

    public BooleanProperty oscuroProperty() {
        return oscuro;
    }

    public void alternar() {
        oscuro.set(!oscuro.get());
    }

    private void aplicar() {
        Application.setUserAgentStylesheet(oscuro.get()
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (raiz != null) {
            raiz.getStyleClass().remove("dark");
            if (oscuro.get()) {
                raiz.getStyleClass().add("dark");
            }
        }
    }
}
