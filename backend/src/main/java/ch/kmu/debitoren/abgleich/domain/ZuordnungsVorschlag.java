package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Pruefe;

/**
 * Vorschlag einer Strategie.
 *
 * @param betrag was zugeordnet würde: der Rest der Zahlung, höchstens der offene Betrag der Rechnung
 * @param automatisch ob der Abgleich den Vorschlag ohne Rückfrage verbuchen darf
 * @param begruendung kurzer Satz für die Klärungsliste
 */
public record ZuordnungsVorschlag(
        OffenerPosten posten, Geld betrag, String regel, boolean automatisch, String begruendung) {

    public ZuordnungsVorschlag {
        Pruefe.vorhanden(posten, "Rechnung");
        Pruefe.vorhanden(betrag, "Betrag");
        Pruefe.pflichttext(regel, "Regel", 40);
    }
}
