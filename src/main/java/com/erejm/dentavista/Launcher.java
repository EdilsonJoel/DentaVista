package com.erejm.dentavista;

import javafx.application.Application;

/**
 * Punto de entrada. Se separa de {@link JavaFxApplication} para que el JAR
 * ejecutable arranque JavaFX sin exigir el module-path.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public class Launcher {

    public static void main(String[] args) {
        // Antialiasing de escena 3D y texto nítido en pantallas HiDPI.
        System.setProperty("prism.lcdtext", "false");
        Application.launch(JavaFxApplication.class, args);
    }
}
