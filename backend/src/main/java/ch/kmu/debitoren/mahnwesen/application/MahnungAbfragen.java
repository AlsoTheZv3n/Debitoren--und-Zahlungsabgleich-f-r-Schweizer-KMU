package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.MahnungZeile;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsuebersicht;
import ch.kmu.debitoren.mahnwesen.domain.MahnwesenFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mahnungen für die Liste im Mahnwesen und die Rechnungsansicht. */
@Service
@Transactional(readOnly = true)
public class MahnungAbfragen {

    private final Mahnungsuebersicht mahnungen;
    private final Clock uhr;

    MahnungAbfragen(Mahnungsuebersicht mahnungen, Clock uhr) {
        this.mahnungen = mahnungen;
        this.uhr = uhr;
    }

    @PreAuthorize("hasRole('LESER')")
    public Page<MahnungZeile> suchen(Mahnungsstatus status, UUID rechnungId, String suchtext, Pageable seite) {
        MandantKontext.erforderlich();
        return mahnungen.suche(status, rechnungId, suchtext, seite);
    }

    @PreAuthorize("hasRole('LESER')")
    public MahnungZeile lesen(UUID id) {
        MandantKontext.erforderlich();
        return mahnungen.zeile(id).orElseThrow(() -> nichtGefunden(id));
    }

    @PreAuthorize("hasRole('LESER')")
    public List<MahnungZeile> derRechnung(UUID rechnungId) {
        MandantKontext.erforderlich();
        return mahnungen.derRechnung(rechnungId);
    }

    /** Heute in Schweizer Zeit; daraus ergeben sich die Tage über Fälligkeit. */
    public LocalDate heute() {
        return LocalDate.now(uhr);
    }

    static FachlicherFehler nichtGefunden(UUID id) {
        return new FachlicherFehler(
                MahnwesenFehlercode.MAHNUNG_NICHT_GEFUNDEN, "Mahnung %s nicht gefunden".formatted(id));
    }
}
