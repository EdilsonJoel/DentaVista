package com.dentavista.dominio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Vademécum odontológico básico: los medicamentos que más se recetan en consulta,
 * con su indicación habitual y su grupo farmacológico para detectar alergias y
 * contraindicaciones (p. ej. amoxicilina en alérgicos a la penicilina, AINE en
 * pacientes anticoagulados o embarazadas).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public enum Medicamento {

    AMOXICILINA("Amoxicilina 500 mg", "1 cápsula cada 8 horas por 7 días", Grupo.PENICILINA),
    AMOXI_CLAVULANICO("Amoxicilina + ácido clavulánico 875/125 mg", "1 tableta cada 12 horas por 7 días", Grupo.PENICILINA),
    CLINDAMICINA("Clindamicina 300 mg", "1 cápsula cada 8 horas por 7 días", Grupo.OTRO),
    AZITROMICINA("Azitromicina 500 mg", "1 tableta cada 24 horas por 3 días", Grupo.MACROLIDO),
    METRONIDAZOL("Metronidazol 500 mg", "1 tableta cada 8 horas por 7 días", Grupo.OTRO),
    IBUPROFENO("Ibuprofeno 400 mg", "1 tableta cada 8 horas por 3 días, después de comer", Grupo.AINE),
    NAPROXENO("Naproxeno 550 mg", "1 tableta cada 12 horas por 3 días, después de comer", Grupo.AINE),
    KETOROLACO("Ketorolaco 10 mg", "1 tableta cada 8 horas por 2 días, máximo 5 días", Grupo.AINE),
    PARACETAMOL("Paracetamol 500 mg", "1 tableta cada 8 horas si hay dolor", Grupo.OTRO),
    DEXAMETASONA("Dexametasona 4 mg", "1 tableta cada 24 horas por 3 días", Grupo.CORTICOIDE),
    CLORHEXIDINA("Clorhexidina 0.12 % enjuague bucal", "Enjuagar 15 ml por 30 segundos, 2 veces al día por 10 días", Grupo.OTRO);

    /** Grupos farmacológicos relevantes para alergias y contraindicaciones. */
    public enum Grupo { PENICILINA, MACROLIDO, AINE, CORTICOIDE, OTRO }

    private final String nombre;
    private final String indicacion;
    private final Grupo grupo;

    Medicamento(String nombre, String indicacion, Grupo grupo) {
        this.nombre = nombre;
        this.indicacion = indicacion;
        this.grupo = grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIndicacion() {
        return indicacion;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    /** Busca por nombre (el odontólogo también puede escribir un medicamento libre). */
    public static Optional<Medicamento> porNombre(String texto) {
        String t = normalizar(texto);
        for (Medicamento m : values()) {
            if (normalizar(m.nombre).equals(t)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /**
     * Alertas de un medicamento para un paciente según sus alergias y antecedentes.
     *
     * @param texto nombre del medicamento (del vademécum o escrito a mano)
     */
    public static List<String> alertas(String texto, String alergias, String antecedentes) {
        List<String> r = new ArrayList<>();
        String med = normalizar(texto);
        String al = normalizar(alergias);
        String ant = normalizar(antecedentes);
        Grupo g = porNombre(texto).map(Medicamento::getGrupo).orElse(Grupo.OTRO);
        String principio = med.split(" ")[0];

        if (!al.isEmpty()) {
            boolean directo = !principio.isEmpty() && al.contains(principio);
            boolean porGrupo = switch (g) {
                case PENICILINA -> contiene(al, "penicilin", "amoxicilin", "betalact", "ampicilin");
                case MACROLIDO -> contiene(al, "azitromicin", "eritromicin", "claritromicin", "macrolid");
                case AINE -> contiene(al, "aine", "ibuprofen", "naproxen", "ketorolac", "aspirin", "diclofenac", "antiinflamatori");
                case CORTICOIDE -> contiene(al, "corticoid", "dexametason", "prednison");
                case OTRO -> false;
            };
            if (directo || porGrupo) {
                r.add("ALERGIA: el paciente es alérgico a " + alergias.trim() + " y "
                        + texto.trim() + (porGrupo && !directo ? " pertenece al mismo grupo (" + nombreGrupo(g) + ")." : "."));
            }
        }
        if (g == Grupo.AINE && contiene(ant, "anticoagul", "warfarin", "sangrado", "hemofil")) {
            r.add("PRECAUCIÓN: AINE en paciente con anticoagulación o riesgo de sangrado. Considera paracetamol.");
        }
        if (g == Grupo.AINE && contiene(ant, "embaraz", "gestant")) {
            r.add("PRECAUCIÓN: los AINE no se recomiendan en el embarazo. Considera paracetamol.");
        }
        if (g == Grupo.CORTICOIDE && contiene(ant, "diabet")) {
            r.add("PRECAUCIÓN: el corticoide puede elevar la glucosa en un paciente diabético.");
        }
        if (g == Grupo.AINE && contiene(ant, "hipertens", "renal", "gastritis", "ulcera")) {
            r.add("PRECAUCIÓN: AINE en paciente con " + antecedentes.trim().toLowerCase() + ".");
        }
        return r;
    }

    private static String nombreGrupo(Grupo g) {
        return switch (g) {
            case PENICILINA -> "penicilinas";
            case MACROLIDO -> "macrólidos";
            case AINE -> "antiinflamatorios no esteroideos";
            case CORTICOIDE -> "corticoides";
            case OTRO -> "otro";
        };
    }

    private static boolean contiene(String texto, String... claves) {
        for (String c : claves) {
            if (texto.contains(c)) {
                return true;
            }
        }
        return false;
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
