package ch.kmu.debitoren.abgleich.application;

import ch.kmu.debitoren.abgleich.domain.AbgleichFehlercode;
import ch.kmu.debitoren.abgleich.domain.OffenePostenAbfrage;
import ch.kmu.debitoren.abgleich.domain.ZahlungZeile;
import ch.kmu.debitoren.abgleich.domain.Zahlungsuebersicht;
import ch.kmu.debitoren.abgleich.domain.Zuordnung;
import ch.kmu.debitoren.abgleich.domain.ZuordnungRepository;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Zahlungen mit ihrem Rest und ihre Zuordnungen, für Klärungsliste und Rechnungsansicht. */
@Service
@Transactional(readOnly = true)
public class Klaerungsliste {

    private final Zahlungsuebersicht zahlungen;
    private final ZuordnungRepository zuordnungen;
    private final OffenePostenAbfrage posten;

    Klaerungsliste(Zahlungsuebersicht zahlungen, ZuordnungRepository zuordnungen, OffenePostenAbfrage posten) {
        this.zahlungen = zahlungen;
        this.zuordnungen = zuordnungen;
        this.posten = posten;
    }

    @PreAuthorize("hasRole('LESER')")
    public Page<ZahlungZeile> zahlungen(Zahlungsstatus status, UUID zahlungsdateiId, String suchtext, Pageable seite) {
        MandantKontext.erforderlich();
        return zahlungen.suche(status, zahlungsdateiId, suchtext, seite);
    }

    @PreAuthorize("hasRole('LESER')")
    public ZahlungZeile zahlung(UUID id) {
        MandantKontext.erforderlich();
        return zahlungen
                .zeile(id)
                .orElseThrow(() -> new FachlicherFehler(
                        AbgleichFehlercode.ZAHLUNG_NICHT_GEFUNDEN, "Zahlung %s nicht gefunden".formatted(id)));
    }

    @PreAuthorize("hasRole('LESER')")
    public List<ZuordnungZeile> zuordnungenDerZahlung(UUID zahlungId) {
        MandantKontext.erforderlich();
        return zuordnungen.fuerZahlung(zahlungId).stream().map(this::zeile).toList();
    }

    @PreAuthorize("hasRole('LESER')")
    public List<ZuordnungZeile> zuordnungenDerRechnung(UUID rechnungId) {
        MandantKontext.erforderlich();
        return zuordnungen.fuerRechnung(rechnungId).stream().map(this::zeile).toList();
    }

    private ZuordnungZeile zeile(Zuordnung zuordnung) {
        var rechnung = posten.finde(zuordnung.getRechnungId());
        var zahlung = zahlungen.zeile(zuordnung.getZahlungId());
        return new ZuordnungZeile(
                zuordnung.getId(),
                zuordnung.getZahlungId(),
                zuordnung.getRechnungId(),
                rechnung.map(r -> r.rechnungsnummer()).orElse(null),
                rechnung.map(r -> r.kundeName()).orElse(null),
                zuordnung.getBetrag(),
                zuordnung.getArt(),
                zuordnung.getRegel(),
                zuordnung.getErstelltVon(),
                zuordnung.getErstelltAm(),
                zahlung.map(ZahlungZeile::valuta).orElse(null),
                zahlung.map(ZahlungZeile::auftraggeber).orElse(null));
    }
}
