package com.erejm.dentavista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Contexto de Spring (persistencia y servicios). La interfaz la arranca {@link Launcher}.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@SpringBootApplication
public class DentaVistaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DentaVistaApplication.class, args);
    }
}
