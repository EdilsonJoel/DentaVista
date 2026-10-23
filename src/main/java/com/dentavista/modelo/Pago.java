package com.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Abono de un paciente a su plan de tratamiento. Nunca se borra: se anula. */
@Entity
@Table(name = "pago",
        uniqueConstraints = @UniqueConstraint(name = "uk_pago_recibo", columnNames = "recibo"),
        indexes = @Index(name = "ix_pago_paciente", columnList = "paciente_id"))
/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "recibo", "monto"})
@EqualsAndHashCode(of = "id")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** R-2026-0001. */
    @Column(nullable = false, length = 13)
    private String recibo;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private MedioPago medio;

    @Size(max = 160)
    @Column(length = 160)
    private String nota;

    @Size(max = 80)
    @Column(name = "registrado_por", length = 80)
    private String registradoPor;

    @Builder.Default
    @Column(nullable = false)
    private boolean anulado = false;

    @Size(max = 160)
    @Column(name = "motivo_anulacion", length = 160)
    private String motivoAnulacion;
}
