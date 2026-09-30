package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Pruefe;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brieftexte der Mahnstufen mit Platzhaltern in geschweiften Klammern, etwa {@code {rechnungsnummer}}. Beträge
 * und Daten erscheinen in Schweizer Schreibweise: «CHF 1’840.00», «12.09.2026».
 */
public final class Mahntext {

    public static final List<String> PLATZHALTER = List.of("rechnungsnummer", "faelligAm", "offen", "frist", "gebuehr");

    /** Für Mahnungen, deren Stufe ein Admin inzwischen gelöscht hat. */
    public static final String ERSATZ =
            "Unsere Rechnung {rechnungsnummer} war am {faelligAm} fällig und ist noch offen. Bitte überweisen Sie"
                    + " den offenen Betrag von {offen} bis am {frist}.";

    private static final Pattern PLATZHALTER_MUSTER = Pattern.compile("\\{([^{}]*)}");
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Mahntext() {}

    /** Lehnt Platzhalter ab, die der Mahnlauf nicht kennt; ein Tippfehler bliebe sonst im Brief stehen. */
    public static String pruefen(String vorlage) {
        var treffer = PLATZHALTER_MUSTER.matcher(vorlage);
        while (treffer.find()) {
            if (!PLATZHALTER.contains(treffer.group(1))) {
                throw Pruefe.ungueltig("Unbekannter Platzhalter {%s}; erlaubt sind %s"
                        .formatted(
                                treffer.group(1),
                                String.join(
                                        ", ",
                                        PLATZHALTER.stream()
                                                .map(name -> "{" + name + "}")
                                                .toList())));
            }
        }
        return vorlage;
    }

    public static String einsetzen(
            String vorlage, String rechnungsnummer, LocalDate faelligAm, Geld offen, LocalDate frist, Geld gebuehr) {
        var werte = Map.of(
                "rechnungsnummer", rechnungsnummer,
                "faelligAm", datum(faelligAm),
                "offen", betrag(offen),
                "frist", datum(frist),
                "gebuehr", betrag(gebuehr));
        return PLATZHALTER_MUSTER
                .matcher(vorlage)
                .replaceAll(treffer -> Matcher.quoteReplacement(werte.getOrDefault(treffer.group(1), treffer.group())));
    }

    public static String datum(LocalDate datum) {
        return datum.format(DATUM);
    }

    /** Betrag mit Währung und typografischem Apostroph als Tausendertrennzeichen. */
    public static String betrag(Geld geld) {
        return geld.waehrung().getCurrencyCode() + " " + zahl(geld.betrag());
    }

    private static String zahl(BigDecimal betrag) {
        var symbole = new DecimalFormatSymbols(Locale.ROOT);
        symbole.setGroupingSeparator('’');
        symbole.setDecimalSeparator('.');
        return new DecimalFormat("#,##0.00", symbole).format(betrag);
    }
}
