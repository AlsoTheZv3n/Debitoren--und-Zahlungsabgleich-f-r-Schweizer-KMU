package ch.kmu.debitoren.testdaten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.kmu.debitoren.TestTokens;
import ch.kmu.debitoren.TestcontainersConfiguration;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungsImport;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Der Seeder läuft beim Start im Profil dev; application-test.yml hält die Mengen klein. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestTokens.class})
@ActiveProfiles({"test", "dev"})
class TestdatenSeederIT {

    @Autowired
    TestdatenSeeder seeder;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CamtTestdateien testdateien;

    @Autowired
    ZahlungsImport zahlungsImport;

    @Test
    void legtDieDemoMandantenMitDenIdsAusDemRealmAn() {
        var mandanten = jdbc.queryForList(
                "select name || '|' || coalesce(qr_iban, '-') from mandant where id in (?, ?) order by name",
                String.class,
                DemoMandanten.BERNER_WERKSTATT,
                DemoMandanten.SEELAND_GARTENBAU);

        assertThat(mandanten).containsExactly("Berner Werkstatt AG|CH4431999123000889012", "Seeland Gartenbau GmbH|-");
    }

    @Test
    void fuelltJedenMandantenMitKundenUndRechnungen() {
        for (var mandant : DemoMandanten.ALLE) {
            assertThat(anzahl("kunde", mandant.id())).isEqualTo(5);
            assertThat(anzahl("rechnung", mandant.id())).isEqualTo(12);
            assertThat(anzahl("mwst_satz", mandant.id())).isEqualTo(7);
            assertThat(jdbc.queryForObject(
                            "select count(*) from rechnung where mandant_id = ? and status <> 'ENTWURF'",
                            Long.class,
                            mandant.id()))
                    .isPositive();
        }
    }

    @Test
    void einZweiterLaufLegtNichtsDoppeltAn() {
        var dateien = anzahl("zahlungsdatei", DemoMandanten.BERNER_WERKSTATT);

        seeder.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForObject("select count(*) from mandant", Long.class))
                .isEqualTo(DemoMandanten.ALLE.size());
        assertThat(anzahl("kunde", DemoMandanten.BERNER_WERKSTATT)).isEqualTo(5);
        assertThat(anzahl("zahlungsdatei", DemoMandanten.BERNER_WERKSTATT)).isEqualTo(dateien);
    }

    @Test
    void zahlungenLaufenDurchImportUndAbgleich() {
        for (var mandant : DemoMandanten.ALLE) {
            assertThat(anzahl("zahlungsdatei", mandant.id())).isPositive();
            await().atMost(Duration.ofSeconds(30)).until(() -> anzahlMitStatus("zahlung", mandant.id(), "OFFEN") == 0);
            assertThat(anzahlMitStatus("zahlung", mandant.id(), "ZUGEORDNET")).isPositive();
            assertThat(anzahlMitStatus("zahlung", mandant.id(), "KLAERUNG"))
                    .as("mindestens die Zahlung mit unbekannter Referenz")
                    .isPositive();
            assertThat(anzahlMitStatus("rechnung", mandant.id(), "BEZAHLT")).isPositive();
        }
    }

    @Test
    void testdateiEnthaeltJedenFallUndLaesstSichImportieren() {
        ZahlungsdateiZeile ergebnis =
                Systemkontext.ausfuehren(() -> MandantKontext.<ZahlungsdateiZeile, RuntimeException>berechnenFuer(
                        DemoMandanten.SEELAND_GARTENBAU, () -> {
                            var datei = testdateien.erzeugen();
                            assertThat(datei.dateiname())
                                    .startsWith("camt054-testdatei-")
                                    .endsWith(".xml");
                            return zahlungsImport.importieren(datei.dateiname(), datei.inhalt());
                        }));

        assertThat(ergebnis.anzahlNeu()).isEqualTo(ergebnis.anzahlBuchungen()).isPositive();
        assertThat(ergebnis.anzahlUebersprungen()).isZero();
    }

    private long anzahlMitStatus(String tabelle, UUID mandant, String status) {
        return jdbc.queryForObject(
                "select count(*) from " + tabelle + " where mandant_id = ? and status = ?",
                Long.class,
                mandant,
                status);
    }

    private long anzahl(String tabelle, UUID mandant) {
        return jdbc.queryForObject("select count(*) from " + tabelle + " where mandant_id = ?", Long.class, mandant);
    }
}
