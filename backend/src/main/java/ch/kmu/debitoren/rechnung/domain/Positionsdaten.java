package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Pruefe;
import java.math.BigDecimal;

/** Eingaben für eine Position; der MWST-Satz ist schon für das Rechnungsdatum aufgelöst. */
public record Positionsdaten(
        String beschreibung, BigDecimal menge, String einheit, BigDecimal einzelpreis, MwstSatz mwstSatz) {

    public Positionsdaten {
        beschreibung = Pruefe.pflichttext(beschreibung, "Beschreibung", 200);
        Pruefe.vorhanden(menge, "Menge");
        if (menge.signum() <= 0) {
            throw Pruefe.ungueltig("Die Menge muss grösser als 0 sein");
        }
        if (menge.stripTrailingZeros().scale() > 3) {
            throw Pruefe.ungueltig("Die Menge hat höchstens drei Nachkommastellen");
        }
        einheit = Pruefe.optionalerText(einheit, "Einheit", 10);
        Pruefe.vorhanden(einzelpreis, "Einzelpreis");
        if (einzelpreis.signum() < 0) {
            throw Pruefe.ungueltig("Der Einzelpreis darf nicht negativ sein");
        }
        if (einzelpreis.stripTrailingZeros().scale() > 2) {
            throw Pruefe.ungueltig("Der Einzelpreis hat höchstens zwei Nachkommastellen");
        }
        Pruefe.vorhanden(mwstSatz, "MWST-Satz");
    }
}
