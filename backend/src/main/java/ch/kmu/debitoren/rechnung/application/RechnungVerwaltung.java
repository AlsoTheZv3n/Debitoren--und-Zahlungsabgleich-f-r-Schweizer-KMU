package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.QrReferenz;
import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung;
import ch.kmu.debitoren.rechnung.domain.ScorReferenz;
import ch.kmu.debitoren.rechnung.domain.Zahlungsreferenz;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.nummern.Nummernvergabe;
import ch.kmu.debitoren.stammdaten.application.KundeInfo;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RechnungVerwaltung {

    static final String NUMMERNKREIS = "RECHNUNG";

    private final RechnungRepository rechnungen;
    private final EntwurfAufloesung aufloesung;
    private final KundenAbfragen kunden;
    private final MandantenAbfragen mandanten;
    private final Nummernvergabe nummern;
    private final Rechnungsdarstellung darstellung;
    private final ZahlteilFabrik zahlteile;
    private final ApplicationEventPublisher ereignisse;
    private final Clock uhr;

    RechnungVerwaltung(
            RechnungRepository rechnungen,
            EntwurfAufloesung aufloesung,
            KundenAbfragen kunden,
            MandantenAbfragen mandanten,
            Nummernvergabe nummern,
            Rechnungsdarstellung darstellung,
            ZahlteilFabrik zahlteile,
            ApplicationEventPublisher ereignisse,
            Clock uhr) {
        this.rechnungen = rechnungen;
        this.aufloesung = aufloesung;
        this.kunden = kunden;
        this.mandanten = mandanten;
        this.nummern = nummern;
        this.darstellung = darstellung;
        this.zahlteile = zahlteile;
        this.ereignisse = ereignisse;
        this.uhr = uhr;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Rechnung entwurfAnlegen(Entwurf entwurf) {
        MandantKontext.erforderlich();
        var kunde = kunde(entwurf);
        var rechnung = Rechnung.entwurf(
                kunde.id(),
                EntwurfAufloesung.empfaenger(kunde),
                aufloesung.kopf(entwurf, kunde),
                aufloesung.positionen(entwurf),
                mandanten.aktueller().rundungFuenfRappen());
        return rechnungen.speichern(rechnung);
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Rechnung entwurfAendern(UUID id, long version, Entwurf entwurf) {
        var rechnung = laden(id, version);
        var kunde = kunde(entwurf);
        rechnung.entwurfAendern(
                kunde.id(),
                EntwurfAufloesung.empfaenger(kunde),
                aufloesung.kopf(entwurf, kunde),
                aufloesung.positionen(entwurf),
                mandanten.aktueller().rundungFuenfRappen());
        return rechnung;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public void entwurfLoeschen(UUID id, long version) {
        var rechnung = laden(id, version);
        rechnung.pruefeLoeschbar();
        rechnungen.loeschen(rechnung);
    }

    /**
     * Gibt einen Entwurf frei: fortlaufende Nummer, Zahlungsreferenz passend zum Konto des Mandanten, Prüfung
     * des QR-Zahlteils. Das PDF entsteht nach dem Commit über das Ereignis {@link RechnungFreigegeben}.
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Rechnung freigeben(UUID id, long version) {
        var rechnung = laden(id, version);
        var kunde = kunden.info(rechnung.getKundeId());
        var steller = zahlteile.steller();

        int jahr = rechnung.getRechnungsdatum().getYear();
        long laufnummer = nummern.naechste(NUMMERNKREIS, jahr);
        var nummer = "RE-%d-%04d".formatted(jahr, laufnummer);
        var grundlage = "%d%06d".formatted(jahr, laufnummer);
        Zahlungsreferenz referenz =
                steller.konto().istQrIban() ? QrReferenz.aus(grundlage) : ScorReferenz.aus(grundlage);

        rechnung.freigeben(nummer, referenz, EntwurfAufloesung.empfaenger(kunde), Instant.now(uhr));
        var verstoesse = darstellung.zahlteilPruefen(zahlteile.fuer(rechnung, steller, kunde));
        if (!verstoesse.isEmpty()) {
            throw new FachlicherFehler(
                    RechnungFehlercode.ZAHLTEIL_UNGUELTIG,
                    "Der QR-Zahlteil wäre ungültig: " + String.join("; ", verstoesse));
        }
        ereignisse.publishEvent(new RechnungFreigegeben(MandantKontext.erforderlich(), rechnung.getId(), nummer));
        return rechnung;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Rechnung stornieren(UUID id, long version, String grund) {
        var rechnung = laden(id, version);
        rechnung.stornieren(grund, Instant.now(uhr));
        return rechnung;
    }

    private Rechnung laden(UUID id, long version) {
        MandantKontext.erforderlich();
        var rechnung = rechnungen.finde(id).orElseThrow(() -> RechnungAbfragen.nichtGefunden(id));
        if (!Objects.equals(rechnung.getVersion(), version)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Die Rechnung wurde inzwischen geändert. Bitte neu laden.");
        }
        return rechnung;
    }

    private KundeInfo kunde(Entwurf entwurf) {
        return kunden.info(Pruefe.vorhanden(entwurf.kundeId(), "Kunde"));
    }
}
