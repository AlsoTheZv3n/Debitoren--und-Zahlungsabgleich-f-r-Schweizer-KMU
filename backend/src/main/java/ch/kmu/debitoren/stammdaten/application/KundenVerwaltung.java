package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.nummern.Nummernvergabe;
import ch.kmu.debitoren.stammdaten.domain.Kunde;
import ch.kmu.debitoren.stammdaten.domain.KundeRepository;
import ch.kmu.debitoren.stammdaten.domain.StammdatenFehlercode;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KundenVerwaltung {

    static final String NUMMERNKREIS = "KUNDE";
    private static final int ERSTE_KUNDENNUMMER = 10_000;

    private final KundeRepository kunden;
    private final Nummernvergabe nummern;

    KundenVerwaltung(KundeRepository kunden, Nummernvergabe nummern) {
        this.kunden = kunden;
        this.nummern = nummern;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Kunde anlegen(KundeDaten daten) {
        MandantKontext.erforderlich();
        var kundennummer = daten.kundennummer() == null || daten.kundennummer().isBlank()
                ? naechsteFreieKundennummer()
                : daten.kundennummer().strip();
        if (kunden.findeNachKundennummer(kundennummer).isPresent()) {
            throw new FachlicherFehler(
                    StammdatenFehlercode.KUNDENNUMMER_VERGEBEN,
                    "Die Kundennummer %s ist bereits vergeben.".formatted(kundennummer));
        }
        return kunden.speichern(
                new Kunde(kundennummer, daten.name(), daten.adresse(), daten.email(), daten.zahlungsfristTage()));
    }

    /** Ändert einen Kunden; {@code version} ist der Stand, den der Benutzer bearbeitet hat. */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Kunde aendern(UUID id, long version, KundeDaten daten) {
        MandantKontext.erforderlich();
        var kunde = kunden.finde(id).orElseThrow(() -> KundenAbfragen.nichtGefunden(id));
        if (!Objects.equals(kunde.getVersion(), version)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Der Kunde wurde inzwischen geändert. Bitte neu laden.");
        }
        kunde.aendern(daten.name(), daten.adresse(), daten.email(), daten.zahlungsfristTage());
        return kunde;
    }

    /** Überspringt Nummern, die jemand von Hand vergeben hat. */
    private String naechsteFreieKundennummer() {
        String kandidat;
        do {
            kandidat = "K-" + (ERSTE_KUNDENNUMMER + nummern.naechste(NUMMERNKREIS, 0));
        } while (kunden.findeNachKundennummer(kandidat).isPresent());
        return kandidat;
    }
}
