package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.MahnbareRechnung;
import ch.kmu.debitoren.mahnwesen.domain.Mahnlauf;
import ch.kmu.debitoren.mahnwesen.domain.MahnlaufRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnregel;
import ch.kmu.debitoren.mahnwesen.domain.MahnstufeRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnung;
import ch.kmu.debitoren.mahnwesen.domain.MahnungRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnverlauf;
import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.security.AngemeldeterBenutzer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Der Mahnlauf: verwirft Vorschläge zu Rechnungen, die nicht mehr offen sind, und schlägt für jede überfällige
 * Rechnung die nächste Stufe vor. Versandt wird erst nach der Freigabe. Täglich startet ihn die
 * {@link MahnlaufPlanung}, von Hand ein Admin.
 */
@Service
public class Mahnlaeufe {

    private final MahnstufeRepository stufen;
    private final MahnungRepository mahnungen;
    private final MahnlaufRepository laeufe;
    private final OffenePostenAbfragen posten;
    private final Clock uhr;

    Mahnlaeufe(
            MahnstufeRepository stufen,
            MahnungRepository mahnungen,
            MahnlaufRepository laeufe,
            OffenePostenAbfragen posten,
            Clock uhr) {
        this.stufen = stufen;
        this.mahnungen = mahnungen;
        this.laeufe = laeufe;
        this.posten = posten;
        this.uhr = uhr;
    }

    /** Heute in Schweizer Zeit; der Stichtag jedes regulären Laufs. */
    public LocalDate heute() {
        return LocalDate.now(uhr);
    }

    /**
     * @param stichtag bis zu diesem Tag gilt eine Rechnung als überfällig; das Mahndatum neuer Vorschläge
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Mahnlauf ausfuehren(LocalDate stichtag) {
        MandantKontext.erforderlich();
        var jetzt = Instant.now(uhr);
        var benutzer = AngemeldeterBenutzer.name();

        int verworfen = 0;
        for (var vorschlag : mahnungen.vorschlaege()) {
            var rechnung = posten.lesen(vorschlag.getRechnungId());
            if (rechnung.isEmpty() || !rechnung.get().istZahlbar()) {
                vorschlag.verwerfen(grund(rechnung.map(Posten::status).orElse(null)), null, benutzer, jetzt);
                verworfen++;
            }
        }

        var stufenliste = stufen.alle();
        var ueberfaellige = posten.ueberfaellige(stichtag);
        var bisher = mahnungen
                .fuerRechnungen(ueberfaellige.stream().map(Posten::rechnungId).toList())
                .stream()
                .collect(Collectors.groupingBy(Mahnung::getRechnungId));
        int neu = 0;
        for (var rechnung : ueberfaellige) {
            var verlauf = Mahnverlauf.aus(bisher.getOrDefault(rechnung.rechnungId(), List.of()));
            var stufe = Mahnregel.naechsteStufe(stufenliste, verlauf, rechnung.faelligAm(), stichtag);
            if (stufe.isPresent()) {
                mahnungen.speichern(Mahnung.vorschlag(mahnbar(rechnung), stufe.get(), stichtag, jetzt));
                neu++;
            }
        }
        return laeufe.speichern(new Mahnlauf(stichtag, jetzt, benutzer, ueberfaellige.size(), neu, verworfen));
    }

    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public List<Mahnlauf> letzte(int anzahl) {
        MandantKontext.erforderlich();
        return laeufe.letzte(anzahl);
    }

    static MahnbareRechnung mahnbar(Posten rechnung) {
        return new MahnbareRechnung(
                rechnung.rechnungId(),
                rechnung.rechnungsnummer(),
                rechnung.kundeId(),
                rechnung.kundeName(),
                rechnung.faelligAm(),
                rechnung.offen());
    }

    static String grund(Rechnungsstatus status) {
        return switch (status) {
            case null -> "Rechnung nicht mehr vorhanden";
            case BEZAHLT -> "Rechnung inzwischen bezahlt";
            case STORNIERT -> "Rechnung storniert";
            default -> "Rechnung nicht mehr offen";
        };
    }
}
