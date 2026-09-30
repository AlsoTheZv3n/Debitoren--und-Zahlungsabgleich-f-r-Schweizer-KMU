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
 * Zahlungsstand der Rechnungen. Das Modul Abgleich ruft das in seiner Transaktion auf, zusammen mit der
 * Zuordnung; eine Zuordnung ändert Zahlung und Rechnung so atomar.
 */
@Service
public class Zahlungseingaenge {

    private final RechnungRepository rechnungen;

    Zahlungseingaenge(RechnungRepository rechnungen) {
        this.rechnungen = rechnungen;
    }

    /**
     * @param bezahlt Summe aller Zuordnungen zu dieser Rechnung nach der Änderung
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional(propagation = Propagation.MANDATORY)
    public void zahlungsstandSetzen(UUID rechnungId, Geld bezahlt) {
        MandantKontext.erforderlich();
        rechnungen
                .zumVerbuchen(rechnungId)
                .orElseThrow(() -> RechnungAbfragen.nichtGefunden(rechnungId))
                .zahlungsstandSetzen(bezahlt);
    }
}
