package ch.kmu.debitoren.mahnwesen.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

/** Die Stufen der Projektplanung: Erinnerung nach 10 Tagen, 1. Mahnung nach 30, 2. Mahnung nach 45. */
class MahnregelTest {

    private static final List<Mahnstufe> STUFEN = StandardMahnstufen.neueStufen();
    private static final LocalDate FAELLIG = LocalDate.of(2026, 8, 1);

    @Test
    void ohneMahnungFolgtNachZehnTagenDieErinnerung() {
        assertThat(naechste(Mahnverlauf.OHNE_MAHNUNG, FAELLIG.plusDays(9))).isNull();
        assertThat(naechste(Mahnverlauf.OHNE_MAHNUNG, FAELLIG.plusDays(10))).isEqualTo(1);
        // Auch lange überfällige Rechnungen beginnen mit der ersten Stufe.
        assertThat(naechste(Mahnverlauf.OHNE_MAHNUNG, FAELLIG.plusDays(200))).isEqualTo(1);
    }

    @Test
    void naechsteStufeErstNachTagenUndMindestabstand() {
        var nachErinnerung = new Mahnverlauf(1, FAELLIG.plusDays(25), false, null);
        // 30 Tage überfällig, aber erst 5 Tage seit der Erinnerung.
        assertThat(naechste(nachErinnerung, FAELLIG.plusDays(30))).isNull();
        assertThat(naechste(nachErinnerung, FAELLIG.plusDays(38))).isNull();
        assertThat(naechste(nachErinnerung, FAELLIG.plusDays(39))).isEqualTo(2);
    }

    @Test
    void nachDerHoechstenStufeKommtNichtsMehr() {
        var nachLetzter = new Mahnverlauf(3, FAELLIG.plusDays(60), false, null);
        assertThat(naechste(nachLetzter, FAELLIG.plusDays(400))).isNull();
    }

    @Test
    void offenerVorschlagUndSperreVerhindernNeueVorschlaege() {
        assertThat(naechste(new Mahnverlauf(0, null, true, null), FAELLIG.plusDays(40)))
                .isNull();
        var gesperrt = new Mahnverlauf(0, null, false, FAELLIG.plusDays(40));
        assertThat(naechste(gesperrt, FAELLIG.plusDays(40))).isNull();
        assertThat(naechste(gesperrt, FAELLIG.plusDays(41))).isEqualTo(1);
    }

    @Property
    void nieMehrAlsEineStufeWeiter(
            @ForAll @IntRange(min = 0, max = 3) int letzteStufe, @ForAll @IntRange(min = 0, max = 500) int tage) {
        var verlauf = letzteStufe == 0 ? Mahnverlauf.OHNE_MAHNUNG : new Mahnverlauf(letzteStufe, FAELLIG, false, null);
        var stufe = naechste(verlauf, FAELLIG.plusDays(tage));
        if (stufe != null) {
            assertThat(stufe).isEqualTo(letzteStufe + 1);
            assertThat(tage).isGreaterThanOrEqualTo(STUFEN.get(stufe - 1).getTageNachFaelligkeit());
        }
    }

    @Test
    void verlaufZaehltNurFreigegebeneUndVersandteMahnungen() {
        var rechnung = new MahnbareRechnung(
                UUID.randomUUID(), "RE-2026-0001", UUID.randomUUID(), "Keller AG", FAELLIG, Geld.chf("100.00"));
        var jetzt = Instant.parse("2026-09-30T08:00:00Z");
        var erste = Mahnung.vorschlag(rechnung, STUFEN.get(0), FAELLIG.plusDays(10), jetzt);
        erste.freigeben(
                Geld.chf("100.00"),
                Geld.chf("0.00"),
                Geld.chf("0.00"),
                Geld.chf("100.00"),
                "Text",
                null,
                "buchhaltung",
                jetzt);
        var verworfen = Mahnung.vorschlag(rechnung, STUFEN.get(1), FAELLIG.plusDays(30), jetzt);
        verworfen.verwerfen("Zahlung zugesagt", FAELLIG.plusDays(45), "buchhaltung", jetzt);

        var verlauf = Mahnverlauf.aus(List.of(erste, verworfen));
        assertThat(verlauf).isEqualTo(new Mahnverlauf(1, FAELLIG.plusDays(10), false, FAELLIG.plusDays(45)));
        assertThat(Mahnverlauf.aus(List.of())).isEqualTo(Mahnverlauf.OHNE_MAHNUNG);
    }

    @Test
    void ersteStufeIgnoriertDenMindestabstand() {
        var stufe = new Mahnstufe(
                1, new Mahnstufendaten("Erinnerung", 10, 30, 10, BigDecimal.ZERO, "Rechnung {rechnungsnummer}"));
        assertThat(stufe.getMindestabstandTage()).isZero();
    }

    private static Integer naechste(Mahnverlauf verlauf, LocalDate stichtag) {
        return Mahnregel.naechsteStufe(STUFEN, verlauf, FAELLIG, stichtag)
                .map(Mahnstufe::getStufe)
                .orElse(null);
    }
}
