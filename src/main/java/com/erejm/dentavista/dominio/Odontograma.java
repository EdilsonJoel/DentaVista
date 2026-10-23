package com.erejm.dentavista.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Estado completo de la boca de un paciente en un instante dado.
 * <p>
 * No se guarda "el odontograma" como tal: se guardan los <b>registros clinicos</b>
 * (hallazgos con fecha) y el odontograma se <b>reconstruye</b> aplicandolos en orden
 * cronologico. Asi se puede ver la boca del paciente en cualquier fecha del pasado
 * (la linea de tiempo de la ficha) sin duplicar datos.
 * </p>
 * Reglas al aplicar un registro:
 * <ul>
 *   <li>Registro de <b>cara</b>: fija el estado de esa cara; {@code SANO} la limpia.</li>
 *   <li>Registro de <b>pieza</b>: fija el estado general; {@code SANO} restablece la pieza
 *       entera. Ausente, implante y corona borran las caras porque ya no existen.</li>
 * </ul>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Odontograma {

    /** Un hallazgo clinico, independiente de la persistencia. */
    public record Registro(int pieza, Superficie superficie, EstadoDental estado,
                           LocalDateTime fecha, long secuencia) {
        public Registro {
            Pieza.validar(pieza);
        }
    }

    /** Una patologia pendiente de tratar (entra al plan de tratamiento). */
    public record Patologia(int pieza, Superficie superficie, EstadoDental estado) {
    }

    private final Map<Integer, EstadoPieza> piezas = new TreeMap<>();

    private Odontograma() {
        for (int fdi : Pieza.TODAS) {
            piezas.put(fdi, new EstadoPieza(fdi));
        }
    }

    /** Odontograma sano, sin registros. */
    public static Odontograma vacio() {
        return new Odontograma();
    }

    /** Reconstruye la boca con todos los registros hasta {@code hasta} inclusive (null = todos). */
    public static Odontograma reconstruir(Collection<Registro> registros, LocalDateTime hasta) {
        Odontograma o = new Odontograma();
        registros.stream()
                .filter(r -> hasta == null || !r.fecha().isAfter(hasta))
                .sorted(Comparator.comparing(Registro::fecha).thenComparingLong(Registro::secuencia))
                .forEach(o::aplicar);
        return o;
    }

    public static Odontograma reconstruir(Collection<Registro> registros) {
        return reconstruir(registros, null);
    }

    private void aplicar(Registro r) {
        piezas.get(r.pieza()).aplicar(r.superficie(), r.estado());
    }

    public EstadoPieza pieza(int fdi) {
        Pieza.validar(fdi);
        return piezas.get(fdi);
    }

    public Collection<EstadoPieza> piezas() {
        return Collections.unmodifiableCollection(piezas.values());
    }

    /** Todo lo que requiere tratamiento, en orden de pieza. */
    public List<Patologia> patologias() {
        List<Patologia> lista = new ArrayList<>();
        for (EstadoPieza p : piezas.values()) {
            if (p.general() != null && p.general().esPatologia()) {
                lista.add(new Patologia(p.fdi(), Superficie.PIEZA, p.general()));
            }
            p.caras.forEach((cara, estado) -> {
                if (estado.esPatologia()) {
                    lista.add(new Patologia(p.fdi(), cara, estado));
                }
            });
        }
        return lista;
    }

    /** Cuantas piezas presentan cada estado (para indicadores). */
    public Map<EstadoDental, Integer> resumen() {
        Map<EstadoDental, Integer> conteo = new EnumMap<>(EstadoDental.class);
        for (EstadoPieza p : piezas.values()) {
            if (p.general() != null) {
                conteo.merge(p.general(), 1, Integer::sum);
            }
            p.caras.values().stream().distinct()
                    .forEach(e -> conteo.merge(e, 1, Integer::sum));
        }
        return conteo;
    }

    /** Piezas presentes en boca (no ausentes ni extraidas). */
    public int piezasPresentes() {
        return (int) piezas.values().stream().filter(p -> !p.ausente()).count();
    }

    /** Estado de una pieza: estado general y estado por cara. */
    public static final class EstadoPieza {

        private final int fdi;
        private EstadoDental general;
        private final EnumMap<Superficie, EstadoDental> caras = new EnumMap<>(Superficie.class);

        private EstadoPieza(int fdi) {
            this.fdi = fdi;
        }

        private void aplicar(Superficie superficie, EstadoDental estado) {
            if (superficie == Superficie.PIEZA) {
                if (estado == EstadoDental.SANO) {
                    general = null;
                    caras.clear();
                    return;
                }
                general = estado;
                if (estado.anulaCaras()) {
                    caras.clear();
                }
                return;
            }
            if (estado == EstadoDental.SANO) {
                caras.remove(superficie);
            } else {
                caras.put(superficie, estado);
            }
        }

        public int fdi() {
            return fdi;
        }

        /** Estado de la pieza completa, o null si no tiene ninguno. */
        public EstadoDental general() {
            return general;
        }

        /** Estado registrado en una cara concreta, o null. */
        public EstadoDental cara(Superficie s) {
            return caras.get(s);
        }

        public Map<Superficie, EstadoDental> caras() {
            return Collections.unmodifiableMap(caras);
        }

        /**
         * Estado con el que se <b>pinta</b> una cara: si la pieza esta ausente, con
         * implante o corona se pinta toda de ese color; si no, la cara usa su propio
         * hallazgo y, sin hallazgo, el estado general (p. ej. endodoncia) o sano.
         */
        public EstadoDental visible(Superficie s) {
            if (general != null && general.anulaCaras()) {
                return general;
            }
            EstadoDental propio = caras.get(s);
            if (propio != null) {
                return propio;
            }
            return general != null ? general : EstadoDental.SANO;
        }

        public boolean ausente() {
            return general == EstadoDental.AUSENTE;
        }

        public boolean sana() {
            return general == null && caras.isEmpty();
        }

        /** Texto corto para listas: "Caries O, M · Endodoncia realizada". */
        public String descripcion() {
            if (sana()) {
                return "Sana";
            }
            List<String> partes = new ArrayList<>();
            Map<EstadoDental, List<String>> porEstado = new EnumMap<>(EstadoDental.class);
            caras.forEach((c, e) -> porEstado.computeIfAbsent(e, k -> new ArrayList<>()).add(c.sigla(fdi)));
            porEstado.forEach((e, siglas) -> partes.add(e.getEtiqueta() + " " + String.join(", ", siglas)));
            if (general != null) {
                partes.add(general.getEtiqueta());
            }
            return String.join(" · ", partes);
        }
    }
}
