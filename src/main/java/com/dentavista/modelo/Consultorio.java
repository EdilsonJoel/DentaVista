package com.dentavista.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del consultorio que usa DentaVista (cabecera de los PDF, recordatorios y
 * pantallas). Se registran en el asistente de primer uso y los edita el administrador.
 * Hay una sola fila.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "consultorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del consultorio es obligatorio")
    @Size(max = 80, message = "El nombre del consultorio admite hasta 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @Pattern(regexp = "^$|\\d{11}", message = "El RUC debe tener 11 dígitos")
    @Column(length = 11)
    private String ruc;

    @Size(max = 160)
    @Column(length = 160)
    private String direccion;

    @Pattern(regexp = "^$|[0-9 +()-]{6,30}", message = "El teléfono solo admite dígitos, espacios y + ( ) -")
    @Column(length = 30)
    private String telefono;

    @Email(message = "El correo del consultorio no tiene un formato válido")
    @Size(max = 120)
    @Column(length = 120)
    private String email;
}
