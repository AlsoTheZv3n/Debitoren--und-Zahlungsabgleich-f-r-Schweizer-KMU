package ch.kmu.debitoren.shared.domain;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;

/** Kleine Prüfungen für Konstruktoren von Entitäten und Wertobjekten. */
public final class Pruefe {

    private Pruefe() {}

    /** Verlangt einen nicht leeren Text bis zur Maximallänge und gibt ihn ohne Randleerzeichen zurück. */
    public static String pflichttext(String wert, String feld, int maxLaenge) {
        var text = optionalerText(wert, feld, maxLaenge);
        if (text == null) {
            throw ungueltig(feld + " fehlt");
        }
        return text;
    }

    /** Wie {@link #pflichttext}, macht aber aus leeren Texten {@code null}. */
    public static String optionalerText(String wert, String feld, int maxLaenge) {
        if (wert == null || wert.isBlank()) {
            return null;
        }
        var text = wert.strip();
        if (text.length() > maxLaenge) {
            throw ungueltig("%s ist länger als %d Zeichen".formatted(feld, maxLaenge));
        }
        return text;
    }

    public static int imBereich(int wert, String feld, int min, int max) {
        if (wert < min || wert > max) {
            throw ungueltig("%s muss zwischen %d und %d liegen".formatted(feld, min, max));
        }
        return wert;
    }

    public static <T> T vorhanden(T wert, String feld) {
        if (wert == null) {
            throw ungueltig(feld + " fehlt");
        }
        return wert;
    }

    public static FachlicherFehler ungueltig(String meldung) {
        return new FachlicherFehler(AllgemeinerFehlercode.UNGUELTIGER_WERT, meldung);
    }
}
