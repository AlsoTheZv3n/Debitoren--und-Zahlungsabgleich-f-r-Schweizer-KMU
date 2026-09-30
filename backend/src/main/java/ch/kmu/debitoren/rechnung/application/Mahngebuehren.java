package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gebühren freigegebener Mahnungen. Das Mahnwesen ruft das in der Transaktion der Freigabe auf; die Version der
 * Rechnung steigt dabei, eine gleichzeitige Zuordnung scheitert also an der Version statt Beträge zu verlieren.
 */
@Service
public class Mahngebuehren {

    private final RechnungRepository rechnungen;

    Mahngebuehren(RechnungRepository rechnungen) {
        this.rechnungen = rechnungen;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional(propagation = Propagation.MANDATORY)
    public void belasten(UUID rechnungId, Geld gebuehr) {
        MandantKontext.erforderlich();
        rechnungen
                .zumVerbuchen(rechnungId)
                .orElseThrow(() -> RechnungAbfragen.nichtGefunden(rechnungId))
                .mahngebuehrBelasten(gebuehr);
    }
}
