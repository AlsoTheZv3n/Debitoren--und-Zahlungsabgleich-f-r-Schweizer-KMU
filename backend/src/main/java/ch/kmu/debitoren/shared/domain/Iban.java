package ch.kmu.debitoren.shared.domain;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Schweizer oder liechtensteinische IBAN, wie sie der QR-Zahlteil für das Konto des Zahlungsempfängers
 * verlangt. Eine QR-IBAN erkennt man an der Institutsidentifikation (IID) 30000 bis 31999.
 */
public record Iban(String wert) {

    private static final Pattern FORMAT = Pattern.compile("(CH|LI)[0-9]{2}[0-9]{5}[0-9A-Z]{12}");
    private static final BigInteger SIEBENUNDNEUNZIG = BigInteger.valueOf(97);

    public Iban {
        Pruefe.vorhanden(wert, "IBAN");
        wert = wert.replace(" ", "").toUpperCase();
        if (!FORMAT.matcher(wert).matches()) {
            throw Pruefe.ungueltig("IBAN muss eine Schweizer oder liechtensteinische IBAN mit 21 Zeichen sein");
        }
        if (!pruefzifferStimmt(wert)) {
            throw Pruefe.ungueltig("Prüfziffer der IBAN stimmt nicht");
        }
    }

    public boolean istQrIban() {
        int iid = Integer.parseInt(wert.substring(4, 9));
        return iid >= 30000 && iid <= 31999;
    }

    /** IBAN in Vierergruppen, wie sie auf Rechnungen gedruckt wird. */
    public String formatiert() {
        return wert.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private static boolean pruefzifferStimmt(String iban) {
        var umgestellt = iban.substring(4) + iban.substring(0, 4);
        var ziffern = new StringBuilder();
        for (char zeichen : umgestellt.toCharArray()) {
            ziffern.append(Character.getNumericValue(zeichen));
        }
        return new BigInteger(ziffern.toString()).mod(SIEBENUNDNEUNZIG).intValue() == 1;
    }

    @Override
    public String toString() {
        return wert;
    }
}
