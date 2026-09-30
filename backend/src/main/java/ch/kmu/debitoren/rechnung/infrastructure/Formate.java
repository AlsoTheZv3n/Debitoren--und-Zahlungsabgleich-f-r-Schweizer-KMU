package ch.kmu.debitoren.rechnung.infrastructure;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Schweizer Schreibweisen für das Rechnungs-PDF: 1’840.00, 12.09.2026, 8.1 %. */
final class Formate {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Formate() {}

    static String betrag(BigDecimal betrag) {
        return zahl("#,##0.00").format(betrag);
    }

    static String menge(BigDecimal menge) {
        return zahl("#,##0.###").format(menge);
    }

    static String prozent(BigDecimal prozent) {
        return zahl("0.0##").format(prozent) + " %";
    }

    static String datum(LocalDate datum) {
        return datum.format(DATUM);
    }

    /** DecimalFormat ist nicht threadsicher und wird deshalb je Aufruf erzeugt. */
    private static DecimalFormat zahl(String muster) {
        var symbole = new DecimalFormatSymbols(Locale.ROOT);
        symbole.setGroupingSeparator('’');
        symbole.setDecimalSeparator('.');
        var format = new DecimalFormat(muster, symbole);
        format.setParseBigDecimal(true);
        return format;
    }
}
