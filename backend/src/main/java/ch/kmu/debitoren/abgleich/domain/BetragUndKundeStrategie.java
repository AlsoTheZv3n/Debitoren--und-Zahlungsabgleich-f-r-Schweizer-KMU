package ch.kmu.debitoren.abgleich.domain;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ohne passende Referenz: Der offene Betrag einer Rechnung entspricht genau dem Rest der Zahlung, und der
 * Auftraggeber heisst wie der Kunde. Das ergibt nur einen Vorschlag; zuordnen muss ein Mensch.
 */
@Component
@Order(2)
public class BetragUndKundeStrategie implements ZuordnungsStrategie {

    public static final String REGEL = "BETRAG_UND_KUNDE";

    private final OffenePostenAbfrage posten;

    public BetragUndKundeStrategie(OffenePostenAbfrage posten) {
        this.posten = posten;
    }

    @Override
    public String regel() {
        return REGEL;
    }

    @Override
    public Optional<ZuordnungsVorschlag> vorschlagen(Zahlungsfall fall) {
        if (fall.auftraggeber() == null || !fall.rest().istPositiv()) {
            return Optional.empty();
        }
        return posten.zahlbareMitOffenemBetrag(fall.rest()).stream()
                .filter(kandidat -> Namensvergleich.passen(kandidat.kundeName(), fall.auftraggeber()))
                .findFirst()
                .map(treffer -> new ZuordnungsVorschlag(
                        treffer,
                        fall.rest(),
                        REGEL,
                        false,
                        "Betrag und Auftraggeber passen zu Rechnung %s".formatted(treffer.rechnungsnummer())));
    }
}
