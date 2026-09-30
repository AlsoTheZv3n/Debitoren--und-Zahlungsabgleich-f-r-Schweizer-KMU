package ch.kmu.debitoren.mahnwesen.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MahnungTest {

    private static final Instant JETZT = Instant.parse("2026-09-30T08:00:00Z");
    private static final LocalDate HEUTE = LocalDate.of(2026, 9, 30);
    private static final Mahnstufe ZWEITE_MAHNUNG =
            StandardMahnstufen.neueStufen().get(2);

    @Test
    void vorschlagUebernimmtFristUndGebuehrDerStufe() {
        var mahnung = vorschlag(Geld.eur("250.00"));

        assertThat(mahnung.getStatus()).isEqualTo(Mahnungsstatus.VORSCHLAG);
        assertThat(mahnung.getStufe()).isEqualTo(3);
        assertThat(mahnung.getBezeichnung()).isEqualTo("2. Mahnung");
        assertThat(mahnung.getMahndatum()).isEqualTo(HEUTE);
        assertThat(mahnung.getFristBis()).isEqualTo(HEUTE.plusDays(10));
        // Die Gebühr gilt in der Währung der Rechnung.
        assertThat(mahnung.getGebuehr()).isEqualTo(Geld.eur("20.00"));
        assertThat(mahnung.getOffenerBetrag()).isNull();
    }

    @Test
    void freigabeHaeltDenStandFestUndWaehltDenVersandweg() {
        var perEmail = vorschlag(Geld.chf("250.00"));
        freigeben(perEmail, "keller@beispiel.example");
        assertThat(perEmail.getStatus()).isEqualTo(Mahnungsstatus.FREIGEGEBEN);
        assertThat(perEmail.getVersandweg()).isEqualTo(Versandweg.EMAIL);
        assertThat(perEmail.getOffenerBetrag()).isEqualTo(Geld.chf("270.00"));
        assertThat(perEmail.getMahngebuehren()).isEqualTo(Geld.chf("20.00"));
        assertThat(perEmail.getBrieftext()).isEqualTo("Bitte bezahlen.");
        assertThat(perEmail.getFreigegebenVon()).isEqualTo("buchhaltung");

        var perPost = vorschlag(Geld.chf("250.00"));
        freigeben(perPost, "  ");
        assertThat(perPost.getVersandweg()).isEqualTo(Versandweg.POST);
        assertThat(perPost.getEmpfaengerEmail()).isNull();
    }

    @Test
    void nurVorschlaegeLassenSichFreigebenOderVerwerfen() {
        var mahnung = vorschlag(Geld.chf("250.00"));
        freigeben(mahnung, null);

        assertThatThrownBy(() -> freigeben(mahnung, null))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(MahnwesenFehlercode.NUR_VORSCHLAG_MOEGLICH));
        assertThatThrownBy(() -> mahnung.verwerfen(null, null, "buchhaltung", JETZT))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("kein Vorschlag mehr");
    }

    @Test
    void verwerfenMitSperreAbDemMahndatum() {
        var mahnung = vorschlag(Geld.chf("250.00"));
        assertThatThrownBy(() -> mahnung.verwerfen(null, HEUTE.minusDays(1), "buchhaltung", JETZT))
                .isInstanceOf(FachlicherFehler.class);

        mahnung.verwerfen("Kunde hat angerufen", HEUTE.plusDays(14), "buchhaltung", JETZT);
        assertThat(mahnung.getStatus()).isEqualTo(Mahnungsstatus.VERWORFEN);
        assertThat(mahnung.getVerworfenGrund()).isEqualTo("Kunde hat angerufen");
        assertThat(mahnung.getGesperrtBis()).isEqualTo(HEUTE.plusDays(14));
        assertThat(mahnung.getVerworfenAm()).isEqualTo(JETZT);
    }

    @Test
    void versandFehlerBleibtBisZumErfolgreichenVersand() {
        var mahnung = vorschlag(Geld.chf("250.00"));
        assertThatThrownBy(() -> mahnung.versendet(JETZT)).isInstanceOf(FachlicherFehler.class);

        freigeben(mahnung, "keller@beispiel.example");
        mahnung.versandFehlgeschlagen("x".repeat(600));
        assertThat(mahnung.getVersandfehler()).hasSize(500);
        assertThat(mahnung.getStatus()).isEqualTo(Mahnungsstatus.FREIGEGEBEN);

        mahnung.versendet(JETZT);
        assertThat(mahnung.getStatus()).isEqualTo(Mahnungsstatus.VERSENDET);
        assertThat(mahnung.getVersandfehler()).isNull();
        assertThatThrownBy(() -> mahnung.versendet(JETZT))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("bereits versendet");
        assertThatThrownBy(() -> mahnung.versandFehlgeschlagen("zu spät")).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void betraegeInAndererWaehrungOderNegativSindUngueltig() {
        var mahnung = vorschlag(Geld.chf("250.00"));
        assertThatThrownBy(() -> mahnung.freigeben(
                        Geld.eur("250.00"),
                        Geld.chf("0.00"),
                        Geld.chf("20.00"),
                        Geld.chf("270.00"),
                        "Text",
                        null,
                        "buchhaltung",
                        JETZT))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("EUR");
        assertThatThrownBy(() -> mahnung.freigeben(
                        Geld.chf("250.00"),
                        Geld.chf("-1.00"),
                        Geld.chf("20.00"),
                        Geld.chf("270.00"),
                        "Text",
                        null,
                        "buchhaltung",
                        JETZT))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("negativ");
    }

    @Test
    void stufenfolgeMussAufsteigen() {
        var daten = StandardMahnstufen.daten();
        Mahnstufendaten.folgePruefen(daten);

        assertThatThrownBy(() -> Mahnstufendaten.folgePruefen(List.of(daten.get(1), daten.get(0))))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Stufe 2");
        assertThatThrownBy(() -> Mahnstufendaten.folgePruefen(List.of())).isInstanceOf(FachlicherFehler.class);
        var sechs = List.of(daten.get(0), daten.get(0), daten.get(0), daten.get(0), daten.get(0), daten.get(0));
        assertThatThrownBy(() -> Mahnstufendaten.folgePruefen(sechs)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void stufePrueftGebuehrUndText() {
        var gueltig = new Mahnstufendaten("Erinnerung", 10, 0, 10, new BigDecimal("5.5"), "Rechnung {rechnungsnummer}");
        assertThat(new Mahnstufe(1, gueltig).getGebuehr()).isEqualByComparingTo("5.50");

        assertThatThrownBy(
                        () -> new Mahnstufe(1, new Mahnstufendaten("A", 10, 0, 10, new BigDecimal("1000.01"), "Text")))
                .isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new Mahnstufe(1, new Mahnstufendaten("A", 10, 0, 10, new BigDecimal("1.005"), "T")))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Nachkommastellen");
        assertThatThrownBy(() -> new Mahnstufe(1, new Mahnstufendaten("A", 10, 0, 10, BigDecimal.ZERO, "{betrag}")))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("{betrag}");
        assertThatThrownBy(() -> new Mahnstufe(6, gueltig)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void textSetztPlatzhalterInSchweizerSchreibweiseEin() {
        var text = Mahntext.einsetzen(
                "Rechnung {rechnungsnummer} vom {faelligAm}: {offen} bis {frist}, Gebühr {gebuehr}. {unbekannt}",
                "RE-2026-0412",
                LocalDate.of(2026, 9, 12),
                Geld.chf("1840.00"),
                LocalDate.of(2026, 10, 10),
                Geld.chf("20.00"));
        assertThat(text)
                .isEqualTo("Rechnung RE-2026-0412 vom 12.09.2026: CHF 1’840.00 bis 10.10.2026, Gebühr CHF 20.00."
                        + " {unbekannt}");
        assertThat(Mahntext.pruefen(Mahntext.ERSATZ)).isEqualTo(Mahntext.ERSATZ);
    }

    private static Mahnung vorschlag(Geld offen) {
        var rechnung = new MahnbareRechnung(
                UUID.randomUUID(), "RE-2026-0412", UUID.randomUUID(), "Keller AG", HEUTE.minusDays(50), offen);
        return Mahnung.vorschlag(rechnung, ZWEITE_MAHNUNG, HEUTE, JETZT);
    }

    private static void freigeben(Mahnung mahnung, String email) {
        mahnung.freigeben(
                Geld.chf("250.00"),
                Geld.chf("0.00"),
                Geld.chf("20.00"),
                Geld.chf("270.00"),
                "Bitte bezahlen.",
                email,
                "buchhaltung",
                JETZT);
    }
}
