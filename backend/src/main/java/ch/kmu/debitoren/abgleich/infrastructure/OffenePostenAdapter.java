package ch.kmu.debitoren.abgleich.infrastructure;

import ch.kmu.debitoren.abgleich.domain.OffenePostenAbfrage;
import ch.kmu.debitoren.abgleich.domain.OffenerPosten;
import ch.kmu.debitoren.abgleich.domain.Postenzustand;
import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.domain.Geld;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapter zum Modul Rechnung: übersetzt dessen Posten in die Sicht des Abgleichs. */
@Component
class OffenePostenAdapter implements OffenePostenAbfrage {

    private final OffenePostenAbfragen rechnungen;

    OffenePostenAdapter(OffenePostenAbfragen rechnungen) {
        this.rechnungen = rechnungen;
    }

    @Override
    public Optional<OffenerPosten> finde(UUID rechnungId) {
        return rechnungen
                .lesen(rechnungId)
                .filter(OffenePostenAdapter::freigegeben)
                .map(OffenePostenAdapter::alsPosten);
    }

    @Override
    public Optional<OffenerPosten> nachReferenz(String referenz) {
        return rechnungen.nachReferenz(referenz).map(OffenePostenAdapter::alsPosten);
    }

    @Override
    public List<OffenerPosten> zahlbareMitOffenemBetrag(Geld betrag) {
        return rechnungen.zahlbareMitOffenemBetrag(betrag).stream()
                .map(OffenePostenAdapter::alsPosten)
                .toList();
    }

    private static boolean freigegeben(Posten posten) {
        return posten.status() != Rechnungsstatus.ENTWURF;
    }

    static OffenerPosten alsPosten(Posten posten) {
        var zustand = switch (posten.status()) {
            case OFFEN, TEILBEZAHLT -> Postenzustand.ZAHLBAR;
            case BEZAHLT -> Postenzustand.BEZAHLT;
            case STORNIERT, ENTWURF -> Postenzustand.STORNIERT;
        };
        return new OffenerPosten(
                posten.rechnungId(),
                posten.rechnungsnummer(),
                posten.kundeName(),
                zustand,
                posten.faelligAm(),
                posten.total(),
                posten.offen());
    }
}
