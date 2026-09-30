package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.Zahlungsreferenz;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rechnungen aus Sicht des Zahlungseingangs, für Abgleich, Klärungsliste und Testdateien. */
@Service
@Transactional(readOnly = true)
public class OffenePostenAbfragen {

    private final RechnungRepository rechnungen;

    OffenePostenAbfragen(RechnungRepository rechnungen) {
        this.rechnungen = rechnungen;
    }

    @PreAuthorize("hasRole('LESER')")
    public Optional<Posten> lesen(UUID rechnungId) {
        MandantKontext.erforderlich();
        return rechnungen.posten(rechnungId);
    }

    /**
     * Die freigegebene Rechnung mit dieser QR- oder SCOR-Referenz, gleich in welchem Status. Eine Referenz
     * mit falscher Prüfziffer gehört zu keiner Rechnung.
     */
    @PreAuthorize("hasRole('LESER')")
    public Optional<Posten> nachReferenz(String referenz) {
        MandantKontext.erforderlich();
        if (referenz == null || referenz.isBlank()) {
            return Optional.empty();
        }
        Zahlungsreferenz gelesen;
        try {
            gelesen = Zahlungsreferenz.lesen(referenz);
        } catch (FachlicherFehler | IllegalArgumentException e) {
            return Optional.empty();
        }
        return rechnungen.postenNachReferenz(gelesen);
    }

    /** Offene und teilbezahlte Rechnungen, bei denen genau dieser Betrag offen ist; älteste zuerst. */
    @PreAuthorize("hasRole('LESER')")
    public List<Posten> zahlbareMitOffenemBetrag(Geld betrag) {
        MandantKontext.erforderlich();
        return rechnungen.zahlbareMitOffenemBetrag(betrag);
    }

    @PreAuthorize("hasRole('LESER')")
    public Page<Posten> zahlbare(Pageable seite) {
        MandantKontext.erforderlich();
        return rechnungen.zahlbare(seite);
    }

    /** Offene und teilbezahlte Rechnungen, die vor dem Stichtag fällig waren; für den Mahnlauf. */
    @PreAuthorize("hasRole('LESER')")
    public List<Posten> ueberfaellige(LocalDate stichtag) {
        MandantKontext.erforderlich();
        return rechnungen.ueberfaellige(stichtag);
    }
}
