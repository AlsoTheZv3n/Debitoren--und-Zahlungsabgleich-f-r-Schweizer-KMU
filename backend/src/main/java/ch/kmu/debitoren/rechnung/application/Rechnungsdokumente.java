package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungsdokument;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.dokumente.DokumentBereitsAbgelegt;
import ch.kmu.debitoren.shared.dokumente.Dokumentablage;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF und Zahlteil einer Rechnung. Das PDF entsteht einmal nach der Freigabe und wird unveränderlich
 * abgelegt; spätere Änderungen an Mandant oder Kunde verändern es nicht mehr.
 */
@Service
public class Rechnungsdokumente {

    private final RechnungRepository rechnungen;
    private final Dokumentablage ablage;
    private final Rechnungsdarstellung darstellung;
    private final ZahlteilFabrik zahlteile;
    private final KundenAbfragen kunden;

    Rechnungsdokumente(
            RechnungRepository rechnungen,
            Dokumentablage ablage,
            Rechnungsdarstellung darstellung,
            ZahlteilFabrik zahlteile,
            KundenAbfragen kunden) {
        this.rechnungen = rechnungen;
        this.ablage = ablage;
        this.darstellung = darstellung;
        this.zahlteile = zahlteile;
        this.kunden = kunden;
    }

    /** Das abgelegte PDF; fehlt es noch, weil die Erstellung nach der Freigabe läuft, entsteht es jetzt. */
    @PreAuthorize("hasRole('LESER')")
    @Transactional
    public Pdf pdf(UUID id) {
        var rechnung = laden(id);
        if (rechnung.getStatus() == Rechnungsstatus.ENTWURF) {
            throw new FachlicherFehler(
                    RechnungFehlercode.PDF_NUR_NACH_FREIGABE,
                    "Ein Entwurf hat noch kein PDF; die Vorschau zeigt den Zahlteil.");
        }
        var inhalt = rechnung.getPdfSchluessel() == null
                ? ablegen(rechnung)
                : ablage.lesen(rechnung.getPdfSchluessel()).orElseGet(() -> ablegen(rechnung));
        return new Pdf(rechnung.getRechnungsnummer() + ".pdf", inhalt);
    }

    /** Erstellt das PDF nach der Freigabe; mehrfache Aufrufe sind harmlos. */
    @PreAuthorize("hasRole('LESER')")
    @Transactional
    public void pdfErstellen(UUID id) {
        var rechnung = laden(id);
        if (rechnung.getStatus() != Rechnungsstatus.ENTWURF && rechnung.getPdfSchluessel() == null) {
            ablegen(rechnung);
        }
    }

    /** Zahlteil als SVG; bei Entwürfen mit Platzhalter statt Referenz. */
    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public String zahlteilSvg(UUID id) {
        var rechnung = laden(id);
        return darstellung.zahlteilSvg(
                zahlteile.fuer(rechnung, zahlteile.steller(), kunden.info(rechnung.getKundeId())));
    }

    private byte[] ablegen(Rechnung rechnung) {
        var schluessel = "rechnungen/%s/%s.pdf".formatted(MandantKontext.erforderlich(), rechnung.getId());
        var steller = zahlteile.steller();
        var kunde = kunden.info(rechnung.getKundeId());
        var inhalt = darstellung.pdf(new Rechnungsdokument(
                steller, rechnung, kunde.kundennummer(), zahlteile.fuer(rechnung, steller, kunde)));
        try {
            ablage.ablegen(schluessel, inhalt);
        } catch (DokumentBereitsAbgelegt e) {
            // Gleichzeitig erstellt, etwa durch Download und Ereignis: das zuerst abgelegte gilt.
            inhalt = ablage.lesen(schluessel).orElseThrow();
        }
        rechnungen.pdfAbgelegt(rechnung.getId(), schluessel);
        return inhalt;
    }

    private Rechnung laden(UUID id) {
        MandantKontext.erforderlich();
        return rechnungen.finde(id).orElseThrow(() -> RechnungAbfragen.nichtGefunden(id));
    }

    public record Pdf(String dateiname, byte[] inhalt) {}
}
