package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import ch.kmu.debitoren.stammdaten.domain.StammdatenFehlercode;
import java.util.Objects;
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

    /**
     * Ändert den Mandanten, für den gerade gearbeitet wird; {@code version} ist der Stand, den der Admin bearbeitet
     * hat. Bereits freigegebene Rechnungen und ihre PDFs bleiben, wie sie sind.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Mandant aendern(long version, MandantDaten daten) {
        var id = MandantKontext.erforderlich();
        var mandant = mandanten
                .finde(id)
                .orElseThrow(() -> new FachlicherFehler(
                        StammdatenFehlercode.MANDANT_NICHT_GEFUNDEN, "Mandant %s nicht gefunden".formatted(id)));
        if (!Objects.equals(mandant.getVersion(), version)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Der Mandant wurde inzwischen geändert. Bitte neu laden.");
        }
        mandant.aendern(daten.name(), daten.adresse(), daten.iban(), daten.qrIban());
        mandant.rechnungsangabenSetzen(daten.mwstNummer(), daten.email(), daten.rundungFuenfRappen());
        return mandant;
    }
}
