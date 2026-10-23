package com.erejm.dentavista.servicio;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;

/**
 * Valida entidades con Bean Validation y traduce el primer error a {@link NegocioException}.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Component
@RequiredArgsConstructor
public class Validacion {

    private final Validator validator;

    public <T> void validar(T objeto) {
        Set<ConstraintViolation<T>> errores = validator.validate(objeto);
        errores.stream()
                .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .ifPresent(v -> {
                    throw new NegocioException(v.getMessage());
                });
    }
}
