package com.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_login", columnNames = "usuario"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "contrasena")
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 3, max = 30, message = "El usuario debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Solo letras, dígitos, punto, guion y guion bajo")
    @Column(nullable = false, length = 30)
    private String usuario;

    /** Hash {@code sal:sha256}; nunca se guarda en claro. */
    @Column(nullable = false, length = 80)
    private String contrasena;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombre;

    /** Colegiatura del Colegio Odontológico (solo odontólogos). */
    @Size(max = 20)
    @Column(length = 20)
    private String colegiatura;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120)
    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    /**
     * La contraseña es temporal (la asignó el administrador): se pide crear una propia
     * al ingresar. Columna opcional para que las bases anteriores se actualicen solas.
     */
    @Builder.Default
    @Column(name = "debe_cambiar_clave")
    private Boolean debeCambiarClave = false;

    /** Solo para el rol PACIENTE: la ficha a la que tiene acceso. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    public boolean requiereCambioClave() {
        return Boolean.TRUE.equals(debeCambiarClave);
    }

    /** "Dra. Lucía Paredes" -> "LP". */
    public String iniciales() {
        return Paciente.iniciales(nombre);
    }
}
