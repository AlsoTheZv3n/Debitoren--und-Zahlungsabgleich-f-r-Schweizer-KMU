package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Set;

/** Kopfangaben einer Rechnung, unabhängig von den Positionen. */
public record Rechnungskopf(LocalDate rechnungsdatum, int zahlungsfristTage, Currency waehrung, String bemerkung) {

    private static final Set<Currency> ERLAUBTE_WAEHRUNGEN = Set.of(Geld.CHF, Geld.EUR);

    public Rechnungskopf {
        Pruefe.vorhanden(rechnungsdatum, "Rechnungsdatum");
        Pruefe.imBereich(zahlungsfristTage, "Zahlungsfrist", 0, 365);
        Pruefe.vorhanden(waehrung, "Währung");
        if (!ERLAUBTE_WAEHRUNGEN.contains(waehrung)) {
            throw new FachlicherFehler(
                    RechnungFehlercode.WAEHRUNG_NICHT_ERLAUBT, "Der QR-Zahlteil erlaubt nur CHF und EUR.");
        }
        bemerkung = Pruefe.optionalerText(bemerkung, "Bemerkung", 500);
    }

    public LocalDate faelligAm() {
        return rechnungsdatum.plusDays(zahlungsfristTage);
    }
}
