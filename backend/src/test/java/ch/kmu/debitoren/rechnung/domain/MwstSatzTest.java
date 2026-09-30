package ch.kmu.debitoren.rechnung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MwstSatzTest {

    @Test
    void giltImZeitraumEinschliesslichDerGrenzen() {
        var satz = new MwstSatz(
                MwstCode.NORMAL,
                "Normalsatz",
                new BigDecimal("7.7"),
                LocalDate.of(2018, 1, 1),
                LocalDate.of(2023, 12, 31));

        assertThat(satz.giltAm(LocalDate.of(2017, 12, 31))).isFalse();
        assertThat(satz.giltAm(LocalDate.of(2018, 1, 1))).isTrue();
        assertThat(satz.giltAm(LocalDate.of(2023, 12, 31))).isTrue();
        assertThat(satz.giltAm(LocalDate.of(2024, 1, 1))).isFalse();
        assertThat(satz.getSatz()).isEqualByComparingTo("7.70").hasScaleOf(2);
    }

    @Test
    void offenerZeitraumGiltUnbegrenzt() {
        var satz = new MwstSatz(MwstCode.OHNE, "Ohne", BigDecimal.ZERO, LocalDate.of(2018, 1, 1), null);
        assertThat(satz.giltAm(LocalDate.of(2099, 1, 1))).isTrue();
        assertThat(satz.getGueltigBis()).isNull();
    }

    @Test
    void prueftSatzUndZeitraum() {
        var ab = LocalDate.of(2024, 1, 1);
        assertThatThrownBy(() -> new MwstSatz(MwstCode.NORMAL, "X", new BigDecimal("-1"), ab, null))
                .isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new MwstSatz(MwstCode.NORMAL, "X", new BigDecimal("100"), ab, null))
                .isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new MwstSatz(MwstCode.NORMAL, "X", new BigDecimal("8.105"), ab, null))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Nachkommastellen");
        assertThatThrownBy(() -> new MwstSatz(MwstCode.NORMAL, "X", new BigDecimal("8.1"), ab, ab.minusDays(1)))
                .isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void schweizerSaetzeWechselnAmNeujahr2024() {
        var saetze = SchweizerMwstSaetze.neueSaetze();
        assertThat(saetze).hasSize(7);

        assertThat(gueltig(saetze, MwstCode.NORMAL, LocalDate.of(2023, 12, 31))).isEqualByComparingTo("7.7");
        assertThat(gueltig(saetze, MwstCode.NORMAL, LocalDate.of(2024, 1, 1))).isEqualByComparingTo("8.1");
        assertThat(gueltig(saetze, MwstCode.REDUZIERT, LocalDate.of(2026, 9, 30)))
                .isEqualByComparingTo("2.6");
        assertThat(gueltig(saetze, MwstCode.BEHERBERGUNG, LocalDate.of(2026, 9, 30)))
                .isEqualByComparingTo("3.8");
        assertThat(gueltig(saetze, MwstCode.OHNE, LocalDate.of(2026, 9, 30))).isEqualByComparingTo("0");
        assertThat(MwstCode.REDUZIERT.bezeichnung()).isEqualTo("Reduzierter Satz");
    }

    private static BigDecimal gueltig(java.util.List<MwstSatz> saetze, MwstCode code, LocalDate datum) {
        return saetze.stream()
                .filter(satz -> satz.getCode() == code && satz.giltAm(datum))
                .findFirst()
                .orElseThrow()
                .getSatz();
    }
}
