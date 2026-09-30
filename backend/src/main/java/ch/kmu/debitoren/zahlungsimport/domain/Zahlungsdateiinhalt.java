package ch.kmu.debitoren.zahlungsimport.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Was der Parser aus einer Bankdatei liest.
 *
 * @param nachrichtId Kennung der Nachricht, die die Bank vergeben hat
 * @param konten IBAN der Konten, über die die Datei berichtet
 * @param gutschriften gebuchte Gutschriften, jede Transaktion einer Sammelbuchung einzeln
 * @param ignoriert Buchungen, die keine gebuchte Gutschrift sind, etwa Belastungen oder Vormerkungen
 */
public record Zahlungsdateiinhalt(
        String nachrichtId, Instant erstelltAm, Set<String> konten, List<Gutschrift> gutschriften, int ignoriert) {

    public Zahlungsdateiinhalt {
        konten = Set.copyOf(konten);
        gutschriften = List.copyOf(gutschriften);
    }
}
