package ch.kmu.debitoren.rechnung.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FormateTest {

    @Test
    void schweizerSchreibweisen() {
        assertThat(Formate.betrag(new BigDecimal("1840"))).isEqualTo("1’840.00");
        assertThat(Formate.betrag(new BigDecimal("1234567.5"))).isEqualTo("1’234’567.50");
        assertThat(Formate.betrag(new BigDecimal("-0.02"))).isEqualTo("-0.02");
        assertThat(Formate.menge(new BigDecimal("2.500"))).isEqualTo("2.5");
        assertThat(Formate.menge(new BigDecimal("1500.000"))).isEqualTo("1’500");
        assertThat(Formate.prozent(new BigDecimal("8.10"))).isEqualTo("8.1 %");
        assertThat(Formate.prozent(new BigDecimal("0.00"))).isEqualTo("0.0 %");
        assertThat(Formate.datum(LocalDate.of(2026, 9, 1))).isEqualTo("01.09.2026");
    }
}
