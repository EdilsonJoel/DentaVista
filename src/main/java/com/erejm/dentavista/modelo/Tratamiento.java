package com.erejm.dentavista.modelo;

import com.erejm.dentavista.dominio.EstadoDental;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Procedimiento del catálogo del consultorio, con su precio y el estado que deja en la pieza.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Entity
@Table(name = "tratamiento",
        uniqueConstraints = @UniqueConstraint(name = "uk_tratamiento_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"codigo", "nombre"})
@EqualsAndHashCode(of = "id")
public class Tratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 8)
    @Column(nullable = false, length = 8)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String nombre;

    @Size(max = 200)
    @Column(length = 200)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Estado que queda registrado al realizarlo (null = no cambia el odontograma). */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultante", length = 24)
    private EstadoDental estadoResultante;

    /** Se aplica a una cara (obturación) o a la pieza completa (corona). */
    @Column(name = "por_cara", nullable = false)
    private boolean porCara;

    /** Se cobra por pieza; si es falso es un procedimiento general (profilaxis). */
    @Builder.Default
    @Column(name = "requiere_pieza", nullable = false)
    private boolean requierePieza = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
