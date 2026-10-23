package com.dentavista.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Receta emitida por un odontólogo en la consulta.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "receta", indexes = @Index(name = "ix_receta_paciente", columnList = "paciente_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "fecha"})
@EqualsAndHashCode(of = "id")
public class Receta {

    /** Una línea de la receta. */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @Column(nullable = false, length = 100)
        private String medicamento;
        @Column(nullable = false, length = 200)
        private String indicacion;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Usuario odontologo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "consulta_id")
    private Consulta consulta;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "receta_item", joinColumns = @JoinColumn(name = "receta_id"))
    @OrderColumn(name = "orden")
    private List<Item> items = new ArrayList<>();

    @Size(max = 400)
    @Column(length = 400)
    private String indicaciones;

    /** Alertas que el odontólogo vio y aceptó al emitir (queda constancia). */
    @Size(max = 600)
    @Column(name = "alertas_aceptadas", length = 600)
    private String alertasAceptadas;
}
