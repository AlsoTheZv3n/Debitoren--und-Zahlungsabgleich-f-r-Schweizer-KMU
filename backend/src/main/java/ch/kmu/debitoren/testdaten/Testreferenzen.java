package ch.kmu.debitoren.testdaten;

import java.math.BigInteger;

/**
 * Gültige Referenzen, die zu keiner Rechnung gehören, für den Fall «Referenz unbekannt» in Testdateien. Die
 * Prüfziffern folgen denselben Normen wie im Modul Rechnung: Modulo 10 rekursiv für die QR-Referenz, ISO 11649
 * für die SCOR-Referenz.
 */
final class Testreferenzen {

    private static final int[] MODULO_10_REKURSIV = {0, 9, 4, 6, 8, 2, 7, 1, 3, 5};
    private static final BigInteger SIEBENUNDNEUNZIG = BigInteger.valueOf(97);

    private Testreferenzen() {}

    /** 26 Ziffern plus Prüfziffer. */
    static String qrReferenz(String ziffern) {
        var basis = "0".repeat(26 - ziffern.length()) + ziffern;
        int uebertrag = 0;
        for (char zeichen : basis.toCharArray()) {
            uebertrag = MODULO_10_REKURSIV[(uebertrag + Character.digit(zeichen, 10)) % 10];
        }
        return basis + (10 - uebertrag) % 10;
    }

    /** {@code RF}, zwei Prüfziffern, dann die Referenz. */
    static String scorReferenz(String referenz) {
        var umgestellt = new StringBuilder();
        for (char zeichen : (referenz + "RF00").toCharArray()) {
            umgestellt.append(Character.getNumericValue(zeichen));
        }
        int pruefziffer =
                98 - new BigInteger(umgestellt.toString()).mod(SIEBENUNDNEUNZIG).intValue();
        return "RF%02d%s".formatted(pruefziffer, referenz);
    }
}
