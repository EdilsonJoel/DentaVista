package com.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Una visita del paciente al consultorio: agrupa los hallazgos y tratamientos de ese día.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "consulta", indexes = @Index(name = "ix_consulta_paciente", columnList = "paciente_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "fecha", "motivo"})
@EqualsAndHashCode(of = "id")
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "odontologo_id")
    private Usuario odontologo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Size(max = 120)
    @Column(length = 120)
    private String motivo;

    @Size(max = 600)
    @Column(length = 600)
    private String notas;
}
