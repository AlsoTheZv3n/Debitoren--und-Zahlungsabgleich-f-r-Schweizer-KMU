package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.mahnwesen.application.Mahnlaeufe;
import ch.kmu.debitoren.mahnwesen.application.Mahnstufen;
import ch.kmu.debitoren.mahnwesen.application.MahnungAbfragen;
import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe;
import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe.Auswahl;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.zahlungsimport.application.Zahlungsstand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Mahnungen verschiedener Stufen zu den überfälligen Testrechnungen: Mahnläufe an zwei früheren Stichtagen, deren
 * Vorschläge freigegeben und wie echte Mahnungen versandt werden (lokal an Mailpit). Der Lauf von heute bleibt
 * offen, damit das Mahnwesen Vorschläge zum Freigeben zeigt.
 */
@Component
@Profile("dev")
class Mahntestdaten {

    private static final Logger LOG = LoggerFactory.getLogger(Mahntestdaten.class);
    private static final List<Integer> FRUEHERE_STICHTAGE = List.of(45, 20);
    private static final int SEITE = 500;
    private static final Duration WARTEZEIT = Duration.ofMinutes(3);

    private final Mahnstufen mahnstufen;
    private final Mahnlaeufe mahnlaeufe;
    private final MahnungAbfragen mahnungen;
    private final MahnungFreigabe freigabe;
    private final Zahlungsstand zahlungen;
    private final Clock uhr;

    Mahntestdaten(
            Mahnstufen mahnstufen,
            Mahnlaeufe mahnlaeufe,
            MahnungAbfragen mahnungen,
            MahnungFreigabe freigabe,
            Zahlungsstand zahlungen,
            Clock uhr) {
        this.mahnstufen = mahnstufen;
        this.mahnlaeufe = mahnlaeufe;
        this.mahnungen = mahnungen;
        this.freigabe = freigabe;
        this.zahlungen = zahlungen;
        this.uhr = uhr;
    }

    /** Nur wenn der Mandant noch nie gemahnt hat; läuft im Kontext des Mandanten. */
    void erzeugen() {
        mahnstufen.standardAnlegen();
        if (!mahnlaeufe.letzte(1).isEmpty()) {
            return;
        }
        aufAbgleichWarten();
        var heute = LocalDate.now(uhr);
        int freigegeben = 0;
        for (var tageZurueck : FRUEHERE_STICHTAGE) {
            mahnlaeufe.ausfuehren(heute.minusDays(tageZurueck));
            freigegeben += allesFreigeben();
        }
        var heutige = mahnlaeufe.ausfuehren(heute);
        LOG.info(
                "Mahnwesen: {} Mahnungen aus früheren Läufen freigegeben, {} Vorschläge von heute offen",
                freigegeben,
                heutige.getVorschlaege());
    }

    /**
     * Der Abgleich läuft nach dem Import asynchron. Gemahnt wird erst, wenn er alle Zahlungen kennt; sonst träfe
     * der Mahnlauf auch Rechnungen, deren Zahlung schon importiert ist.
     */
    private void aufAbgleichWarten() {
        var frist = Instant.now().plus(WARTEZEIT);
        while (zahlungen.unbearbeitet() > 0 && Instant.now().isBefore(frist)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private int allesFreigeben() {
        var auswahl = new ArrayList<Auswahl>();
        var seite = mahnungen.suchen(Mahnungsstatus.VORSCHLAG, null, null, PageRequest.of(0, SEITE));
        seite.forEach(vorschlag -> auswahl.add(new Auswahl(vorschlag.id(), vorschlag.version())));
        while (seite.hasNext()) {
            seite = mahnungen.suchen(Mahnungsstatus.VORSCHLAG, null, null, seite.nextPageable());
            seite.forEach(vorschlag -> auswahl.add(new Auswahl(vorschlag.id(), vorschlag.version())));
        }
        return auswahl.isEmpty() ? 0 : freigabe.freigeben(auswahl).freigegeben();
    }
}
