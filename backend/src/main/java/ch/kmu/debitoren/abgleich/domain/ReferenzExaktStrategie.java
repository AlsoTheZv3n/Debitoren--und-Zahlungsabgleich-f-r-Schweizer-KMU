package ch.kmu.debitoren.abgleich.domain;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Die Referenz trifft genau eine zahlbare Rechnung in derselben Währung. Als einzige Strategie ordnet sie
 * automatisch zu: Eine QR- oder SCOR-Referenz ist eindeutig und hat eine Prüfziffer.
 */
@Component
@Order(1)
public class ReferenzExaktStrategie implements ZuordnungsStrategie {

    public static final String REGEL = "REFERENZ_EXAKT";

    private final OffenePostenAbfrage posten;

    public ReferenzExaktStrategie(OffenePostenAbfrage posten) {
        this.posten = posten;
    }

    @Override
    public String regel() {
        return REGEL;
    }

    @Override
    public Optional<ZuordnungsVorschlag> vorschlagen(Zahlungsfall fall) {
        if (!fall.hatReferenz() || !fall.rest().istPositiv()) {
            return Optional.empty();
        }
        return posten.nachReferenz(fall.referenz())
                .filter(OffenerPosten::istZahlbar)
                .filter(treffer -> treffer.offen().waehrung().equals(fall.rest().waehrung()))
                .map(treffer -> new ZuordnungsVorschlag(
                        treffer,
                        fall.rest().istGroesserAls(treffer.offen()) ? treffer.offen() : fall.rest(),
                        REGEL,
                        true,
                        "Referenz von Rechnung %s".formatted(treffer.rechnungsnummer())));
    }
}
