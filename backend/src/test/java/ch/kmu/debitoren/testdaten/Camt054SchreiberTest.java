package ch.kmu.debitoren.testdaten;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.rechnung.domain.QrReferenz;
import ch.kmu.debitoren.rechnung.domain.ScorReferenz;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.testdaten.Camt054Schreiber.Testgutschrift;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Testdateien müssen echte camt.054-Dateien sein, sonst prüfen Demos und Seeder nichts. */
class Camt054SchreiberTest {

    @Test
    void schreibtSchemagueltigeDateienMitAllenArtenVonReferenzen() throws Exception {
        var datei = Camt054Schreiber.schreiben(
                "TEST-1",
                Instant.parse("2026-09-15T04:30:00Z"),
                "CH4431999123000889012",
                List.of(
                        new Testgutschrift(
                                "B-1",
                                LocalDate.of(2026, 9, 15),
                                Geld.chf("1840.00"),
                                "000000000000000020260000018",
                                "Keller & Söhne AG",
                                null),
                        new Testgutschrift(
                                "B-2", LocalDate.of(2026, 9, 15), Geld.eur("12.50"), "RF422026000001", null, null),
                        new Testgutschrift(
                                "B-3", LocalDate.of(2026, 9, 14), Geld.chf("95.50"), null, "Gasser", "Rechnung <42>")));

        var schema = SchemaFactory.newDefaultInstance()
                .newSchema(new StreamSource(getClass().getResourceAsStream("/camt/camt.054.001.08.xsd")));
        schema.newValidator().validate(new StreamSource(new ByteArrayInputStream(datei)));

        var text = new String(datei, StandardCharsets.UTF_8);
        assertThat(text)
                .contains("<Prtry>QRR</Prtry>")
                .contains("<Cd>SCOR</Cd>")
                .contains("<Ustrd>Rechnung &lt;42&gt;</Ustrd>")
                .contains("Keller &amp; Söhne AG")
                .contains("<CreDtTm>2026-09-15T06:30:00+02:00</CreDtTm>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026000001", "2026000417", "88880000000001", "9999260930101500"})
    void referenzenHabenDieselbenPruefziffernWieDieRechnungen(String grundlage) {
        assertThat(Testreferenzen.qrReferenz(grundlage))
                .isEqualTo(QrReferenz.aus(grundlage).wert());
        assertThat(Testreferenzen.scorReferenz(grundlage))
                .isEqualTo(ScorReferenz.aus(grundlage).wert());
    }

    @Test
    void teilbetragIstEtwaDieHaelfteAufFuenfRappen() {
        assertThat(CamtTestdateien.teilbetrag(Geld.chf("100.00"))).isEqualTo(Geld.chf("50.00"));
        assertThat(CamtTestdateien.teilbetrag(Geld.chf("95.55"))).isEqualTo(Geld.chf("47.80"));
        assertThat(CamtTestdateien.teilbetrag(Geld.chf("8.00")))
                .as("unter 5 Franken wird voll bezahlt")
                .isEqualTo(Geld.chf("8.00"));
    }
}
