package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.rechnung.application.Entwurf;
import ch.kmu.debitoren.rechnung.application.MwstSaetze;
import ch.kmu.debitoren.rechnung.application.RechnungVerwaltung;
import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.stammdaten.application.KundeDaten;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import ch.kmu.debitoren.stammdaten.application.KundenVerwaltung;
import ch.kmu.debitoren.stammdaten.application.MandantVerwaltung;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import ch.kmu.debitoren.stammdaten.application.NeuerMandant;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Legt beim Start im Profil {@code dev} deterministische Testdaten an, sofern sie fehlen: zwei Mandanten,
 * je Mandant Kunden und Rechnungen über 18 Monate. Der feste Zufalls-Seed sorgt dafür, dass jede Instanz
 * dieselben Daten erhält. Zahlungen kommen als camt.054-Dateien durch den echten Import ({@link Zahlungstestdaten}).
 */
@Component
@Profile("dev")
@EnableConfigurationProperties(TestdatenEigenschaften.class)
class TestdatenSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestdatenSeeder.class);
    private static final int TAGE_18_MONATE = 548;
    private static final int[] ZAHLUNGSFRISTEN = {30, 30, 30, 30, 30, 10, 20, 45, 60};
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final MandantenAbfragen mandantenAbfragen;
    private final MandantVerwaltung mandantVerwaltung;
    private final MwstSaetze mwstSaetze;
    private final KundenAbfragen kundenAbfragen;
    private final KundenVerwaltung kundenVerwaltung;
    private final RechnungVerwaltung rechnungVerwaltung;
    private final Zahlungstestdaten zahlungen;
    private final TestdatenEigenschaften eigenschaften;
    private final Clock uhr;

    TestdatenSeeder(
            MandantenAbfragen mandantenAbfragen,
            MandantVerwaltung mandantVerwaltung,
            MwstSaetze mwstSaetze,
            KundenAbfragen kundenAbfragen,
            KundenVerwaltung kundenVerwaltung,
            RechnungVerwaltung rechnungVerwaltung,
            Zahlungstestdaten zahlungen,
            TestdatenEigenschaften eigenschaften,
            Clock uhr) {
        this.mandantenAbfragen = mandantenAbfragen;
        this.mandantVerwaltung = mandantVerwaltung;
        this.mwstSaetze = mwstSaetze;
        this.kundenAbfragen = kundenAbfragen;
        this.kundenVerwaltung = kundenVerwaltung;
        this.rechnungVerwaltung = rechnungVerwaltung;
        this.zahlungen = zahlungen;
        this.eigenschaften = eigenschaften;
        this.uhr = uhr;
    }

    @Override
    public void run(ApplicationArguments argumente) {
        Systemkontext.ausfuehren(() -> DemoMandanten.ALLE.forEach(this::mandantAnlegen));
        for (var mandant : DemoMandanten.ALLE) {
            Systemkontext.fuerMandant(mandant.id(), () -> {
                fuellen(mandant);
                zahlungen.erzeugen(mandant.id(), eigenschaften.seed());
            });
        }
    }

    private void mandantAnlegen(NeuerMandant mandant) {
        if (!mandantenAbfragen.existiert(mandant.id())) {
            mandantVerwaltung.anlegen(mandant);
            LOG.info("Demo-Mandant angelegt: {}", mandant.name());
        }
    }

    private void fuellen(NeuerMandant mandant) {
        mwstSaetzeAbwarten();
        if (kundenAbfragen.suchen(null, 0, 1).getTotalElements() > 0) {
            return;
        }
        var start = Instant.now();
        var zufall = new Random(eigenschaften.seed() ^ mandant.id().getLeastSignificantBits());
        var faker = new Faker(Locale.of("de", "CH"), zufall);

        var kunden = IntStream.range(0, eigenschaften.kundenJeMandant())
                .mapToObj(i -> kundenVerwaltung.anlegen(kunde(faker, zufall)).getId())
                .toList();

        var katalog = Leistungskatalog.fuer(mandant.id());
        var heute = LocalDate.now(uhr);
        for (int i = 0; i < eigenschaften.rechnungenJeMandant(); i++) {
            rechnung(zufall, katalog, kunden, heute);
        }
        LOG.info(
                "Testdaten für {}: {} Kunden, {} Rechnungen in {} s",
                mandant.name(),
                kunden.size(),
                eigenschaften.rechnungenJeMandant(),
                Duration.between(start, Instant.now()).toSeconds());
    }

    /**
     * Die MWST-Sätze legt das Rechnungsmodul nach dem Ereignis {@code MandantAngelegt} an. Kommt es nicht
     * rechtzeitig, legt der Seeder sie selbst an; der Use Case ist idempotent.
     */
    private void mwstSaetzeAbwarten() {
        var frist = Instant.now().plusSeconds(20);
        while (mwstSaetze.auflisten(null).isEmpty()) {
            if (Instant.now().isAfter(frist)) {
                mwstSaetze.standardSaetzeAnlegen();
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static KundeDaten kunde(Faker faker, Random zufall) {
        boolean firma = zufall.nextInt(100) < 60;
        var name = firma
                ? faker.company().name()
                : faker.name().firstName() + " " + faker.name().lastName();
        var plz = faker.address().zipCode();
        if (!plz.matches("\\d{4}")) {
            plz = String.valueOf(1000 + zufall.nextInt(8600));
        }
        var adresse = Adresse.schweiz(
                kuerzen(faker.address().streetName(), 70),
                String.valueOf(1 + zufall.nextInt(120)),
                plz,
                kuerzen(faker.address().city(), 35));
        var email = (firma ? "buchhaltung@" : "post@") + slug(name) + ".example";
        return new KundeDaten(
                null,
                kuerzen(name, 70),
                adresse,
                kuerzen(email, 254),
                ZAHLUNGSFRISTEN[zufall.nextInt(ZAHLUNGSFRISTEN.length)]);
    }

    private void rechnung(Random zufall, Leistungskatalog katalog, List<UUID> kunden, LocalDate heute) {
        var kundeId = kunden.get(zufall.nextInt(kunden.size()));
        var datum = heute.minusDays(zufall.nextInt(TAGE_18_MONATE));
        var waehrung = zufall.nextInt(100) < 5 ? Geld.EUR : Geld.CHF;
        var positionen = new ArrayList<Entwurf.Position>();
        for (int i = 0, anzahl = 1 + zufall.nextInt(8); i < anzahl; i++) {
            positionen.add(katalog.position(zufall));
        }
        var bemerkung = zufall.nextInt(100) < 25
                ? "Arbeiten gemäss Offerte vom "
                        + datum.minusDays(10 + zufall.nextInt(40)).format(DATUM)
                : null;

        var entwurf =
                rechnungVerwaltung.entwurfAnlegen(new Entwurf(kundeId, datum, null, waehrung, bemerkung, positionen));
        boolean bleibtEntwurf = datum.isAfter(heute.minusDays(10)) && zufall.nextInt(100) < 40;
        if (bleibtEntwurf) {
            return;
        }
        var offen = rechnungVerwaltung.freigeben(entwurf.getId(), entwurf.getVersion());
        if (zufall.nextInt(100) < 3) {
            rechnungVerwaltung.stornieren(offen.getId(), offen.getVersion(), "Doppelt erfasst");
        }
    }

    private static String kuerzen(String text, int laenge) {
        return text.length() <= laenge ? text : text.substring(0, laenge).strip();
    }

    private static String slug(String text) {
        var ohneAkzente = Normalizer.normalize(
                        text.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ohneAkzente
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
