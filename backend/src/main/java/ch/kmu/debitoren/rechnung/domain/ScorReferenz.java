package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.domain.Referenzart;
import java.math.BigInteger;

/**
 * Creditor Reference nach ISO 11649 ({@code RF}, zwei Prüfziffern, bis 21 Zeichen), für Mandanten mit
 * normaler IBAN.
 */
public record ScorReferenz(String wert) implements Zahlungsreferenz {

    private static final BigInteger SIEBENUNDNEUNZIG = BigInteger.valueOf(97);

    public ScorReferenz {
        Pruefe.vorhanden(wert, "SCOR-Referenz");
        wert = wert.replace(" ", "").toUpperCase();
        if (!wert.matches("RF\\d{2}[A-Z0-9]{1,21}")) {
            throw Pruefe.ungueltig("Eine SCOR-Referenz beginnt mit RF, zwei Prüfziffern und bis zu 21 Zeichen");
        }
        if (rest(wert.substring(4) + wert.substring(0, 4)) != 1) {
            throw Pruefe.ungueltig("Prüfziffern der SCOR-Referenz stimmen nicht");
        }
    }

    /** Baut eine Referenz aus bis zu 21 Ziffern und Grossbuchstaben. */
    public static ScorReferenz aus(String grundlage) {
        var text = grundlage.toUpperCase();
        if (!text.matches("[A-Z0-9]{1,21}")) {
            throw Pruefe.ungueltig("Die Grundlage einer SCOR-Referenz sind 1 bis 21 Ziffern oder Buchstaben");
        }
        int pruefziffern = 98 - rest(text + "RF00");
        return new ScorReferenz("RF%02d%s".formatted(pruefziffern, text));
    }

    /** Buchstaben werden zu Zahlen (A=10 … Z=35), dann Modulo 97 wie bei der IBAN. */
    private static int rest(String text) {
        var ziffern = new StringBuilder();
        for (char zeichen : text.toCharArray()) {
            ziffern.append(Character.getNumericValue(zeichen));
        }
        return new BigInteger(ziffern.toString()).mod(SIEBENUNDNEUNZIG).intValue();
    }

    @Override
    public String formatiert() {
        return wert.replaceAll("(.{4})(?!$)", "$1 ");
    }

    @Override
    public Referenzart art() {
        return Referenzart.SCOR;
    }

    @Override
    public String toString() {
        return wert;
    }
}
