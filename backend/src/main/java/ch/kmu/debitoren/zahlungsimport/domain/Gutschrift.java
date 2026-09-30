package ch.kmu.debitoren.zahlungsimport.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Pruefe;
import java.time.LocalDate;

/**
 * Eine Gutschrift, wie sie in der Bankdatei steht, noch ohne Bezug zu Rechnungen.
 *
 * @param bankReferenz eindeutige Referenz der Bank für diese Buchung; erkennt Buchungen, die in mehreren
 *     Dateien vorkommen
 * @param referenz QR- oder SCOR-Referenz ohne Leerzeichen, {@code null} ohne strukturierte Referenz
 */
public record Gutschrift(
        String bankReferenz,
        LocalDate buchungsdatum,
        LocalDate valuta,
        Geld betrag,
        String referenz,
        String auftraggeber,
        String auftraggeberIban,
        String mitteilung) {

    public Gutschrift {
        bankReferenz = Pruefe.pflichttext(bankReferenz, "Bankreferenz", 80);
        Pruefe.vorhanden(valuta, "Valuta");
        Pruefe.vorhanden(betrag, "Betrag");
        if (!betrag.istPositiv()) {
            throw Pruefe.ungueltig("Eine Gutschrift hat einen Betrag über null");
        }
        referenz = referenz == null || referenz.isBlank() ? null : referenz.replace(" ", "");
        auftraggeber = kuerzen(auftraggeber, 140);
        auftraggeberIban = auftraggeberIban == null || auftraggeberIban.isBlank()
                ? null
                : auftraggeberIban.replace(" ", "").toUpperCase();
        mitteilung = kuerzen(mitteilung, 500);
    }

    private static String kuerzen(String text, int laenge) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var bereinigt = text.strip().replaceAll("\\s+", " ");
        return bereinigt.length() <= laenge ? bereinigt : bereinigt.substring(0, laenge);
    }
}
