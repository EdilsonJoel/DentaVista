package com.erejm.dentavista.modelo;

import com.erejm.dentavista.dominio.Pieza;
import com.erejm.dentavista.dominio.Superficie;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una línea del plan de tratamiento (y del presupuesto) de un paciente.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "item_plan", indexes = @Index(name = "ix_item_paciente", columnList = "paciente_id, estado"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "pieza", "superficie", "estado"})
@EqualsAndHashCode(of = "id")
public class ItemPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "tratamiento_id", nullable = false)
    private Tratamiento tratamiento;

    /** Pieza FDI; null en procedimientos generales. */
    private Integer pieza;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Superficie superficie;

    /** Precio congelado al crear el ítem (el catálogo puede cambiar después). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoItem estado = EstadoItem.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime creado;

    private LocalDateTime realizado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @Size(max = 200)
    @Column(length = 200)
    private String nota;

    /** "16 · Oclusal" o "General". */
    public String ubicacion() {
        if (pieza == null) {
            return "General";
        }
        String texto = String.valueOf(pieza);
        if (superficie != null && superficie.esCara()) {
            texto += " · " + superficie.etiqueta(pieza);
        }
        return texto;
    }

    public String descripcionPieza() {
        return pieza == null ? "Procedimiento general" : Pieza.nombre(pieza);
    }
}
