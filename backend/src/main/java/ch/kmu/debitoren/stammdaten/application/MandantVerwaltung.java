package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MandantVerwaltung {

    private final MandantRepository mandanten;
    private final ApplicationEventPublisher ereignisse;

    MandantVerwaltung(MandantRepository mandanten, ApplicationEventPublisher ereignisse) {
        this.mandanten = mandanten;
        this.ereignisse = ereignisse;
    }

    /**
     * Legt einen Mandanten an. Das ist Aufgabe des Betriebs, nicht der Admins eines Mandanten, daher nur im
     * Systemkontext. Andere Module richten sich über das Ereignis {@link MandantAngelegt} ein.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    @Transactional
    public Mandant anlegen(NeuerMandant neu) {
        if (mandanten.existiert(neu.id())) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.BEREITS_VORHANDEN, "Mandant %s existiert bereits".formatted(neu.id()));
        }
        var mandant = new Mandant(neu.id(), neu.name(), neu.adresse(), neu.iban(), neu.qrIban());
        mandant.rechnungsangabenSetzen(neu.mwstNummer(), neu.email(), true);
        var gespeichert = mandanten.speichern(mandant);
        ereignisse.publishEvent(new MandantAngelegt(gespeichert.getId()));
        return gespeichert;
    }
}
