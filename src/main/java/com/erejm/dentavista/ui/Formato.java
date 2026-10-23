package com.erejm.dentavista.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Formatos de fecha y moneda (soles) usados en toda la aplicación.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Formato {

    private static final Locale PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PE);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PE);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PE);
    private static final DateTimeFormatter MES_CORTO = DateTimeFormatter.ofPattern("MMM yy", PE);

    private Formato() {
    }

    public static String soles(BigDecimal monto) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "S/ " + f.format(monto == null ? BigDecimal.ZERO : monto);
    }

    public static String fecha(LocalDate f) {
        return f == null ? "—" : FECHA.format(f);
    }

    public static String fecha(LocalDateTime f) {
        return f == null ? "—" : FECHA.format(f);
    }

    public static String fechaHora(LocalDateTime f) {
        return f == null ? "—" : FECHA_HORA.format(f);
    }

    public static String fechaLarga(LocalDateTime f) {
        return f == null ? "—" : FECHA_LARGA.format(f);
    }

    public static String mesCorto(LocalDateTime f) {
        return MES_CORTO.format(f).replace(".", "");
    }

    /** "hoy", "ayer", "hace 5 días", "hace 3 meses". */
    public static String relativo(LocalDateTime f) {
        if (f == null) {
            return "sin visitas";
        }
        long dias = ChronoUnit.DAYS.between(f.toLocalDate(), LocalDate.now());
        if (dias <= 0) {
            return "hoy";
        }
        if (dias == 1) {
            return "ayer";
        }
        if (dias < 31) {
            return "hace " + dias + " días";
        }
        long meses = dias / 30;
        if (meses < 12) {
            return "hace " + meses + (meses == 1 ? " mes" : " meses");
        }
        long anios = dias / 365;
        return "hace " + anios + (anios == 1 ? " año" : " años");
    }
}
