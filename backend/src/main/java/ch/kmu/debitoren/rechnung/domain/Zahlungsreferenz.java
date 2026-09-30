package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Referenzart;

/** Referenz im QR-Zahlteil, über die der Abgleich eine Zahlung ihrer Rechnung zuordnet. */
public sealed interface Zahlungsreferenz permits QrReferenz, ScorReferenz {

    /** Referenz ohne Leerzeichen, so wie sie im QR-Code und in camt.054 steht. */
    String wert();

    /** Referenz in Gruppen, wie sie auf der Rechnung gedruckt wird. */
    String formatiert();

    Referenzart art();

    /** Erkennt die Art am Präfix: {@code RF} steht für SCOR, sonst QR-Referenz. */
    static Zahlungsreferenz lesen(String wert) {
        var bereinigt = wert.replace(" ", "").toUpperCase();
        return bereinigt.startsWith("RF") ? new ScorReferenz(bereinigt) : new QrReferenz(bereinigt);
    }
}
