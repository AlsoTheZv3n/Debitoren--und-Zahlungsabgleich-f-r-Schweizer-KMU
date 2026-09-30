package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Mahnangaben;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Mahndokument;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Das Mahnschreiben zu einer Rechnung als PDF. Der QR-Zahlteil lautet auf den offenen Betrag samt Gebühren und
 * trägt dieselbe Referenz wie die Rechnung; der Abgleich ordnet die Zahlung so automatisch zu. Adressiert ist
 * das Schreiben an die aktuelle Adresse des Kunden.
 */
@Service
public class Mahnschreiben {

    private final RechnungRepository rechnungen;
    private final KundenAbfragen kunden;
    private final ZahlteilFabrik zahlteile;
    private final Rechnungsdarstellung darstellung;

    Mahnschreiben(
            RechnungRepository rechnungen,
            KundenAbfragen kunden,
            ZahlteilFabrik zahlteile,
            Rechnungsdarstellung darstellung) {
        this.rechnungen = rechnungen;
        this.kunden = kunden;
        this.zahlteile = zahlteile;
        this.darstellung = darstellung;
    }

    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public byte[] pdf(UUID rechnungId, Mahnangaben angaben) {
        MandantKontext.erforderlich();
        var rechnung = rechnungen.finde(rechnungId).orElseThrow(() -> RechnungAbfragen.nichtGefunden(rechnungId));
        if (rechnung.getStatus() == Rechnungsstatus.ENTWURF) {
            throw new FachlicherFehler(RechnungFehlercode.MAHNUNG_NICHT_MOEGLICH, "Ein Entwurf wird nicht gemahnt.");
        }
        var steller = zahlteile.steller();
        var kunde = kunden.info(rechnung.getKundeId());
        var empfaenger = EntwurfAufloesung.empfaenger(kunde);
        return darstellung.mahnungPdf(new Mahndokument(
                steller,
                rechnung,
                kunde.kundennummer(),
                empfaenger,
                angaben,
                zahlteile.fuerMahnung(rechnung, steller, empfaenger, angaben)));
    }
}
