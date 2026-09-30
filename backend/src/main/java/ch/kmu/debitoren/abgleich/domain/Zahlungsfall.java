package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Pruefe;
import java.util.UUID;

/**
 * Was eine Strategie über die Zahlung wissen muss.
 *
 * @param rest Teil der Zahlung, der noch keiner Rechnung zugeordnet ist
 * @param referenz QR- oder SCOR-Referenz ohne Leerzeichen, {@code null} ohne Referenz
 */
public record Zahlungsfall(UUID zahlungId, Geld rest, String referenz, String auftraggeber) {

    public Zahlungsfall {
        Pruefe.vorhanden(zahlungId, "Zahlung");
        Pruefe.vorhanden(rest, "Restbetrag");
        referenz = referenz == null || referenz.isBlank() ? null : referenz;
    }

    public boolean hatReferenz() {
        return referenz != null;
    }
}
