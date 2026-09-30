package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.Mahnung;
import ch.kmu.debitoren.mahnwesen.domain.MahnungRepository;
import ch.kmu.debitoren.rechnung.application.Mahngebuehren;
import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.security.AngemeldeterBenutzer;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gibt Mahnvorschläge frei oder verwirft sie. Mit der Freigabe belastet die Gebühr die Rechnung und das Schreiben
 * hält den Stand fest; versandt wird nach dem Commit über das Ereignis {@link MahnungFreigegeben}.
 */
@Service
public class MahnungFreigabe {

    private final MahnungRepository mahnungen;
    private final OffenePostenAbfragen posten;
    private final Mahngebuehren gebuehren;
    private final KundenAbfragen kunden;
    private final Brieftexte brieftexte;
    private final ApplicationEventPublisher ereignisse;
    private final TransactionTemplate transaktion;
    private final Clock uhr;

    MahnungFreigabe(
            MahnungRepository mahnungen,
            OffenePostenAbfragen posten,
            Mahngebuehren gebuehren,
            KundenAbfragen kunden,
            Brieftexte brieftexte,
            ApplicationEventPublisher ereignisse,
            PlatformTransactionManager transaktionen,
            Clock uhr) {
        this.mahnungen = mahnungen;
        this.posten = posten;
        this.gebuehren = gebuehren;
        this.kunden = kunden;
        this.brieftexte = brieftexte;
        this.ereignisse = ereignisse;
        this.transaktion = new TransactionTemplate(transaktionen);
        this.uhr = uhr;
    }

    /**
     * Gibt die gewählten Vorschläge frei, jeden in einer eigenen Transaktion: Ein Konflikt bei einer Mahnung hält
     * die übrigen nicht auf. Ist eine Rechnung inzwischen bezahlt oder storniert, wird ihr Vorschlag verworfen.
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    public Freigabeergebnis freigeben(List<Auswahl> auswahl) {
        var mandant = MandantKontext.erforderlich();
        var benutzer = AngemeldeterBenutzer.name();
        int freigegeben = 0;
        var abgelehnt = new ArrayList<Ablehnung>();
        for (var gewaehlt : auswahl) {
            try {
                var ablehnung = transaktion.execute(status -> einzeln(gewaehlt, mandant, benutzer));
                if (ablehnung == null) {
                    freigegeben++;
                } else {
                    abgelehnt.add(ablehnung);
                }
            } catch (FachlicherFehler fehler) {
                abgelehnt.add(new Ablehnung(gewaehlt.id(), null, fehler.getMessage()));
            } catch (OptimisticLockingFailureException fehler) {
                abgelehnt.add(new Ablehnung(
                        gewaehlt.id(), null, "Rechnung oder Mahnung wurde gleichzeitig geändert. Bitte neu laden."));
            }
        }
        return new Freigabeergebnis(freigegeben, abgelehnt);
    }

    /** Verwirft einen Vorschlag; mit {@code gesperrtBis} schlägt der Mahnlauf die Rechnung bis dahin nicht vor. */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Mahnung verwerfen(UUID id, long version, String grund, LocalDate gesperrtBis) {
        MandantKontext.erforderlich();
        var mahnung = laden(id, version);
        if (gesperrtBis != null && gesperrtBis.isAfter(LocalDate.now(uhr).plusYears(1))) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.UNGUELTIGER_WERT, "Eine Sperre dauert höchstens ein Jahr.");
        }
        mahnung.verwerfen(grund, gesperrtBis, AngemeldeterBenutzer.name(), Instant.now(uhr));
        return mahnung;
    }

    /** Gibt eine Mahnung frei; liefert den Grund, wenn sie stattdessen verworfen wurde. */
    private Ablehnung einzeln(Auswahl auswahl, UUID mandant, String benutzer) {
        var mahnung = laden(auswahl.id(), auswahl.version());
        var jetzt = Instant.now(uhr);
        var rechnung = posten.lesen(mahnung.getRechnungId()).filter(Posten::istZahlbar);
        if (rechnung.isEmpty()) {
            var grund = Mahnlaeufe.grund(
                    posten.lesen(mahnung.getRechnungId()).map(Posten::status).orElse(null));
            mahnung.verwerfen(grund, null, benutzer, jetzt);
            return new Ablehnung(mahnung.getId(), mahnung.getRechnungsnummer(), grund + "; Vorschlag verworfen.");
        }
        var offen = rechnung.get();
        var gebuehr = mahnung.getGebuehr();
        if (gebuehr.istPositiv()) {
            gebuehren.belasten(offen.rechnungId(), gebuehr);
        }
        var zuZahlen = offen.offen().plus(gebuehr);
        mahnung.freigeben(
                offen.total(),
                offen.bezahlt(),
                offen.mahngebuehren().plus(gebuehr),
                zuZahlen,
                brieftexte.fuer(mahnung, zuZahlen),
                kunden.info(offen.kundeId()).email(),
                benutzer,
                jetzt);
        ereignisse.publishEvent(new MahnungFreigegeben(mandant, mahnung.getId()));
        return null;
    }

    private Mahnung laden(UUID id, long version) {
        var mahnung = mahnungen.finde(id).orElseThrow(() -> MahnungAbfragen.nichtGefunden(id));
        if (!Objects.equals(mahnung.getVersion(), version)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Die Mahnung zu Rechnung %s wurde inzwischen geändert. Bitte neu laden."
                            .formatted(mahnung.getRechnungsnummer()));
        }
        return mahnung;
    }

    /** Ein gewählter Vorschlag mit der Version, die der Benutzer gesehen hat. */
    public record Auswahl(UUID id, long version) {}

    /** @param rechnungsnummer null, wenn die Mahnung nicht geladen werden konnte */
    public record Ablehnung(UUID mahnungId, String rechnungsnummer, String grund) {}

    public record Freigabeergebnis(int freigegeben, List<Ablehnung> nichtFreigegeben) {}
}
