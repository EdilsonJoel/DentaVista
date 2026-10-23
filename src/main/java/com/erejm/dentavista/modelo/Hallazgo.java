package com.erejm.dentavista.modelo;

import com.erejm.dentavista.dominio.EstadoDental;
import com.erejm.dentavista.dominio.Odontograma;
import com.erejm.dentavista.dominio.Superficie;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro clínico sobre una pieza o una de sus caras. Es la única fuente de verdad
 * del odontograma: el estado de la boca se reconstruye a partir de estos registros.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "hallazgo", indexes = @Index(name = "ix_hallazgo_paciente", columnList = "paciente_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "pieza", "superficie", "estado", "fecha"})
@EqualsAndHashCode(of = "id")
public class Hallazgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    /** Pieza en notación FDI (11..48). */
    @Column(nullable = false)
    private int pieza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Superficie superficie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoDental estado;

    @Size(max = 300)
    @Column(length = 300)
    private String nota;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Size(max = 80)
    @Column(name = "registrado_por", length = 80)
    private String registradoPor;

    public Odontograma.Registro aRegistro() {
        return new Odontograma.Registro(pieza, superficie, estado, fecha, id == null ? 0 : id);
    }
}
