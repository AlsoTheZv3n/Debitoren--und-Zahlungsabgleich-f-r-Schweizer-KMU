package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import ch.kmu.debitoren.testdaten.Camt054Schreiber.Testgutschrift;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungsImport;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungsdateienAbfragen;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Zahlungen zu den Testrechnungen: etwa 60 % bezahlt, 10 % teilbezahlt, der Rest offen, dazu je ein Beispiel
 * für die Klärungsliste. Die Zahlungen laufen als camt.054-Dateien je Monat durch den echten Import und den
 * Abgleich, so zeigen die Testdaten genau, was die Anwendung mit echten Bankdateien macht.
 */
@Component
@Profile("dev")
class Zahlungstestdaten {

    private static final Logger LOG = LoggerFactory.getLogger(Zahlungstestdaten.class);
    private static final ZoneId SCHWEIZ = ZoneId.of("Europe/Zurich");
    private static final int SEITE = 200;

    private final OffenePostenAbfragen offenePosten;
    private final MandantenAbfragen mandanten;
    private final ZahlungsImport zahlungsImport;
    private final ZahlungsdateienAbfragen zahlungsdateien;
    private final Clock uhr;

    Zahlungstestdaten(
            OffenePostenAbfragen offenePosten,
            MandantenAbfragen mandanten,
            ZahlungsImport zahlungsImport,
            ZahlungsdateienAbfragen zahlungsdateien,
            Clock uhr) {
        this.offenePosten = offenePosten;
        this.mandanten = mandanten;
        this.zahlungsImport = zahlungsImport;
        this.zahlungsdateien = zahlungsdateien;
        this.uhr = uhr;
    }

    /** Nur wenn der Mandant noch keine Bankdatei hat; läuft im Kontext des Mandanten. */
    void erzeugen(UUID mandantId, long seed) {
        if (zahlungsdateien.anzahl() > 0) {
            return;
        }
        var mandant = mandanten.aktueller();
        var zufall = new Random(seed ^ mandantId.getMostSignificantBits());
        var heute = LocalDate.now(uhr);
        var kennung = mandantId.toString().substring(30);

        var gutschriften = new ArrayList<Testgutschrift>();
        var ohneZahlung = new ArrayList<Posten>();
        for (var posten : alleZahlbaren()) {
            int wurf = zufall.nextInt(100);
            var valuta = frueher(posten.faelligAm().minusDays(zufall.nextInt(21)), heute);
            var bankReferenz = "SEED-%s-%05d".formatted(kennung, gutschriften.size() + 1);
            if (wurf < 60) {
                gutschriften.add(gutschrift(bankReferenz, valuta, posten.offen(), posten));
            } else if (wurf < 70) {
                gutschriften.add(gutschrift(bankReferenz, valuta, CamtTestdateien.teilbetrag(posten.offen()), posten));
            } else {
                ohneZahlung.add(posten);
            }
        }
        gutschriften.addAll(klaerungsfaelle(kennung, ohneZahlung, mandant.referenzart(), heute));

        var jeMonat = new TreeMap<YearMonth, List<Testgutschrift>>();
        gutschriften.stream()
                .sorted(Comparator.comparing(Testgutschrift::valuta))
                .forEach(gutschrift -> jeMonat.computeIfAbsent(
                                YearMonth.from(gutschrift.valuta()), monat -> new ArrayList<>())
                        .add(gutschrift));
        for (Map.Entry<YearMonth, List<Testgutschrift>> monat : jeMonat.entrySet()) {
            var erstellt = frueher(monat.getKey().atEndOfMonth(), heute)
                    .atTime(18, 0)
                    .atZone(SCHWEIZ)
                    .toInstant();
            var inhalt = Camt054Schreiber.schreiben(
                    "SEED-%s-%s".formatted(monat.getKey(), kennung),
                    frueher(erstellt, Instant.now(uhr)),
                    mandant.zahlungskonto().wert(),
                    monat.getValue());
            zahlungsImport.importieren("camt054-%s.xml".formatted(monat.getKey()), inhalt);
        }
        LOG.info(
                "Zahlungen für {}: {} Gutschriften in {} Dateien", mandant.name(), gutschriften.size(), jeMonat.size());
    }

    /**
     * Je ein Fall für die Klärungsliste, aus Rechnungen, die sonst unbezahlt blieben: ohne Referenz mit
     * passendem Betrag und Namen, eine Überzahlung, Euro auf eine Frankenrechnung und eine unbekannte Referenz.
     */
    private static List<Testgutschrift> klaerungsfaelle(
            String kennung, List<Posten> ohneZahlung, Referenzart referenzart, LocalDate heute) {
        var faelle = new ArrayList<Testgutschrift>();
        if (!ohneZahlung.isEmpty()) {
            var posten = ohneZahlung.get(0);
            faelle.add(new Testgutschrift(
                    "SEED-%s-K1".formatted(kennung),
                    heute.minusDays(2),
                    posten.offen(),
                    null,
                    posten.kundeName().toUpperCase(),
                    "Rechnung " + posten.rechnungsnummer()));
        }
        if (ohneZahlung.size() > 1) {
            var posten = ohneZahlung.get(1);
            var zuviel = posten.offen().plus(Geld.von("20.00", posten.offen().waehrung()));
            faelle.add(gutschrift("SEED-%s-K2".formatted(kennung), heute.minusDays(3), zuviel, posten));
        }
        if (ohneZahlung.size() > 2 && Geld.CHF.equals(ohneZahlung.get(2).offen().waehrung())) {
            var posten = ohneZahlung.get(2);
            var inEuro = Geld.eur(posten.offen().betrag().toPlainString());
            faelle.add(gutschrift("SEED-%s-K3".formatted(kennung), heute.minusDays(4), inEuro, posten));
        }
        var unbekannt = referenzart == Referenzart.QR
                ? Testreferenzen.qrReferenz("88880000" + kennung.replaceAll("[^0-9]", "0"))
                : Testreferenzen.scorReferenz("8888" + kennung.toUpperCase());
        faelle.add(new Testgutschrift(
                "SEED-%s-K4".formatted(kennung),
                heute.minusDays(1),
                Geld.chf("250.00"),
                unbekannt,
                "M. Brunner",
                null));
        return faelle;
    }

    private List<Posten> alleZahlbaren() {
        var alle = new ArrayList<Posten>();
        var seite = offenePosten.zahlbare(PageRequest.of(0, SEITE, Sort.unsorted()));
        alle.addAll(seite.getContent());
        while (seite.hasNext()) {
            seite = offenePosten.zahlbare(seite.nextPageable());
            alle.addAll(seite.getContent());
        }
        return alle;
    }

    private static Testgutschrift gutschrift(String bankReferenz, LocalDate valuta, Geld betrag, Posten posten) {
        return new Testgutschrift(bankReferenz, valuta, betrag, posten.referenz(), posten.kundeName(), null);
    }

    private static <T extends Comparable<? super T>> T frueher(T a, T b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
