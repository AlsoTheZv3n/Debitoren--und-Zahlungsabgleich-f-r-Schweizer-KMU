package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.domain.Kunde;
import ch.kmu.debitoren.stammdaten.domain.KundeRepository;
import ch.kmu.debitoren.stammdaten.domain.StammdatenFehlercode;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class KundenAbfragen {

    private final KundeRepository kunden;

    KundenAbfragen(KundeRepository kunden) {
        this.kunden = kunden;
    }

    /** Kunden des Mandanten, nach Name sortiert; der Suchtext trifft Name, Kundennummer und Ort. */
    @PreAuthorize("hasRole('LESER')")
    public Page<Kunde> suchen(String suchtext, int seite, int groesse) {
        MandantKontext.erforderlich();
        var sortierung = Sort.by("name", "kundennummer");
        return kunden.suche(suchtext, PageRequest.of(seite, groesse, sortierung));
    }

    @PreAuthorize("hasRole('LESER')")
    public Kunde lesen(UUID id) {
        MandantKontext.erforderlich();
        return kunden.finde(id).orElseThrow(() -> nichtGefunden(id));
    }

    /** Für andere Module, etwa den Empfänger einer Rechnung. */
    @PreAuthorize("hasRole('LESER')")
    public KundeInfo info(UUID id) {
        var kunde = lesen(id);
        return new KundeInfo(
                kunde.getId(),
                kunde.getKundennummer(),
                kunde.getName(),
                kunde.getAdresse(),
                kunde.getEmail(),
                kunde.getZahlungsfristTage());
    }

    static FachlicherFehler nichtGefunden(UUID id) {
        return new FachlicherFehler(StammdatenFehlercode.KUNDE_NICHT_GEFUNDEN, "Kunde %s nicht gefunden".formatted(id));
    }
}
