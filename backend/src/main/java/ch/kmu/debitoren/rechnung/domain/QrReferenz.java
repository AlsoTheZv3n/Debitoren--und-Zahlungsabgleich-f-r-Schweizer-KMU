package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.domain.Referenzart;

/**
 * QR-Referenz: 27 Ziffern, die letzte ist die Prüfziffer nach Modulo 10 rekursiv. Nur zusammen mit einer
 * QR-IBAN gültig.
 */
public record QrReferenz(String wert) implements Zahlungsreferenz {

    private static final int[] TABELLE = {0, 9, 4, 6, 8, 2, 7, 1, 3, 5};
    private static final int LAENGE = 27;

    public QrReferenz {
        Pruefe.vorhanden(wert, "QR-Referenz");
        wert = wert.replace(" ", "");
        if (!wert.matches("\\d{27}")) {
            throw Pruefe.ungueltig("Eine QR-Referenz besteht aus 27 Ziffern");
        }
        if (wert.chars().allMatch(ziffer -> ziffer == '0')) {
            throw Pruefe.ungueltig("Eine QR-Referenz darf nicht nur aus Nullen bestehen");
        }
        if (pruefziffer(wert.substring(0, LAENGE - 1)) != wert.charAt(LAENGE - 1) - '0') {
            throw Pruefe.ungueltig("Prüfziffer der QR-Referenz stimmt nicht");
        }
    }

    /** Baut eine Referenz aus bis zu 26 Ziffern: links mit Nullen aufgefüllt, rechts die Prüfziffer. */
    public static QrReferenz aus(String ziffern) {
        if (!ziffern.matches("\\d{1,26}")) {
            throw Pruefe.ungueltig("Die Grundlage einer QR-Referenz sind 1 bis 26 Ziffern");
        }
        var basis = "0".repeat(LAENGE - 1 - ziffern.length()) + ziffern;
        return new QrReferenz(basis + pruefziffer(basis));
    }

    static int pruefziffer(String ziffern) {
        int uebertrag = 0;
        for (int i = 0; i < ziffern.length(); i++) {
            uebertrag = TABELLE[(uebertrag + ziffern.charAt(i) - '0') % 10];
        }
        return (10 - uebertrag) % 10;
    }

    /** Wie auf Einzahlungsscheinen: zwei Ziffern, dann Fünfergruppen. */
    @Override
    public String formatiert() {
        var text = new StringBuilder(wert.substring(0, 2));
        for (int i = 2; i < LAENGE; i += 5) {
            text.append(' ').append(wert, i, i + 5);
        }
        return text.toString();
    }

    @Override
    public Referenzart art() {
        return Referenzart.QR;
    }

    @Override
    public String toString() {
        return wert;
    }
}
