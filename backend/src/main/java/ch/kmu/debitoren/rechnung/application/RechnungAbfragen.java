package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.RechnungZeile;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung;
import ch.kmu.debitoren.rechnung.domain.Rechnungsfilter;
import ch.kmu.debitoren.rechnung.domain.Rechnungsrechner;
import ch.kmu.debitoren.rechnung.domain.Summen;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RechnungAbfragen {

    private final RechnungRepository rechnungen;
    private final EntwurfAufloesung aufloesung;
    private final KundenAbfragen kunden;
    private final MandantenAbfragen mandanten;
    private final ZahlteilFabrik zahlteile;
    private final Rechnungsdarstellung darstellung;
    private final Clock uhr;

    RechnungAbfragen(
            RechnungRepository rechnungen,
            EntwurfAufloesung aufloesung,
            KundenAbfragen kunden,
            MandantenAbfragen mandanten,
            ZahlteilFabrik zahlteile,
            Rechnungsdarstellung darstellung,
            Clock uhr) {
        this.rechnungen = rechnungen;
        this.aufloesung = aufloesung;
        this.kunden = kunden;
        this.mandanten = mandanten;
        this.zahlteile = zahlteile;
        this.darstellung = darstellung;
        this.uhr = uhr;
    }

    @PreAuthorize("hasRole('LESER')")
    public Page<RechnungZeile> suchen(Rechnungsfilter filter, Pageable seite) {
        MandantKontext.erforderlich();
        return rechnungen.suche(filter, heute(), seite);
    }

    @PreAuthorize("hasRole('LESER')")
    public Rechnung lesen(UUID id) {
        MandantKontext.erforderlich();
        return rechnungen.finde(id).orElseThrow(() -> nichtGefunden(id));
    }

    /** Total plus Mahngebühren abzüglich Zahlungseingänge; 0, wenn die Rechnung nichts mehr erwartet oder Entwurf ist. */
    @PreAuthorize("hasRole('LESER')")
    public BigDecimal offenerBetrag(UUID id) {
        MandantKontext.erforderlich();
        return rechnungen.posten(id).map(posten -> posten.offen().betrag()).orElse(BigDecimal.ZERO.setScale(2));
    }

    /** Heute in Schweizer Zeit; entscheidet, ob eine offene Rechnung überfällig ist. */
    public LocalDate heute() {
        return LocalDate.now(uhr);
    }

    /**
     * Rechnet einen noch nicht gespeicherten Entwurf durch und zeichnet seinen Zahlteil, für die
     * Live-Vorschau im Editor. Die Referenz ist ein Platzhalter, sie entsteht erst bei der Freigabe.
     */
    @PreAuthorize("hasRole('LESER')")
    public Vorschau vorschau(Entwurf entwurf) {
        MandantKontext.erforderlich();
        var kunde = entwurf.kundeId() == null ? null : kunden.info(entwurf.kundeId());
        var kopf = aufloesung.kopf(entwurf, kunde);
        var posten = aufloesung.positionen(entwurf).stream()
                .map(position -> new Rechnungsrechner.Posten(
                        Rechnungsrechner.positionsbetrag(position.menge(), position.einzelpreis()),
                        position.mwstSatz().getCode(),
                        position.mwstSatz().getSatz()))
                .toList();
        var summen = Rechnungsrechner.berechnen(
                posten, kopf.waehrung(), mandanten.aktueller().rundungFuenfRappen());
        var zahlteil = zahlteile.vorschau(
                zahlteile.steller(),
                kopf.waehrung(),
                summen.total(),
                kunde == null ? null : EntwurfAufloesung.empfaenger(kunde));
        return new Vorschau(
                summen,
                posten.stream().map(Rechnungsrechner.Posten::betrag).toList(),
                darstellung.zahlteilSvg(zahlteil));
    }

    /** Summen, Nettobetrag jeder Position in Eingabereihenfolge und der Zahlteil als SVG. */
    public record Vorschau(Summen summen, List<BigDecimal> positionsbetraege, String zahlteilSvg) {}

    static FachlicherFehler nichtGefunden(UUID id) {
        return new FachlicherFehler(
                RechnungFehlercode.RECHNUNG_NICHT_GEFUNDEN, "Rechnung %s nicht gefunden".formatted(id));
    }
}
