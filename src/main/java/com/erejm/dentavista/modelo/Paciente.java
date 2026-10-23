package com.erejm.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.stream.Collectors;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
@Entity
@Table(name = "paciente",
        uniqueConstraints = @UniqueConstraint(name = "uk_paciente_dni", columnNames = "dni"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "dni", "nombres", "apellidos"})
@EqualsAndHashCode(of = "id")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    @Column(nullable = false, length = 8)
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String apellidos;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** F, M u O (otro / prefiere no decir). */
    @Column(length = 1)
    private String sexo;

    @Pattern(regexp = "^$|[0-9 +()-]{6,20}", message = "El teléfono solo admite dígitos, espacios y + ( ) -")
    @Column(length = 20)
    private String telefono;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    @Size(max = 160)
    @Column(length = 160)
    private String direccion;

    /** Alergias conocidas (se muestran como alerta roja en toda la ficha). */
    @Size(max = 200)
    @Column(length = 200)
    private String alergias;

    /** Antecedentes médicos relevantes: diabetes, hipertensión, anticoagulantes... */
    @Size(max = 400)
    @Column(length = 400)
    private String antecedentes;

    @Size(max = 600)
    @Column(length = 600)
    private String notas;

    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Integer edad() {
        return fechaNacimiento == null ? null : Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public String iniciales() {
        return iniciales(nombres + " " + apellidos);
    }

    /** Primera letra de las dos primeras palabras significativas ("Dra." se omite). */
    static String iniciales(String texto) {
        if (texto == null || texto.isBlank()) {
            return "?";
        }
        return Arrays.stream(texto.trim().split("\\s+"))
                .filter(p -> !p.endsWith("."))
                .limit(2)
                .map(p -> p.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
}
