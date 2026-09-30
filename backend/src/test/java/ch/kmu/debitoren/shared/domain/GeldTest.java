package ch.kmu.debitoren.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeldTest {

    @Test
    void rundetAufZweiStellenKaufmaennisch() {
        assertThat(Geld.chf("10.005").betrag()).isEqualByComparingTo("10.01");
        assertThat(Geld.chf("10.004").betrag()).isEqualByComparingTo("10.00");
        assertThat(Geld.chf("-10.005").betrag()).isEqualByComparingTo("-10.01");
        assertThat(Geld.chf("7").betrag().scale()).isEqualTo(2);
    }

    @Test
    void gleicheBetraegeSindGleichUnabhaengigVonDerSchreibweise() {
        assertThat(Geld.chf("12.5")).isEqualTo(Geld.chf("12.50"));
        assertThat(Geld.chf("12.50")).isNotEqualTo(Geld.eur("12.50"));
    }

    @Test
    void rechnetInDerselbenWaehrung() {
        assertThat(Geld.chf("100.10").plus(Geld.chf("0.95"))).isEqualTo(Geld.chf("101.05"));
        assertThat(Geld.chf("100.00").minus(Geld.chf("250.50"))).isEqualTo(Geld.chf("-150.50"));
        assertThat(Geld.chf("19.99").mal(new BigDecimal("3"))).isEqualTo(Geld.chf("59.97"));
        assertThat(Geld.chf("10.00").mal(new BigDecimal("0.081"))).isEqualTo(Geld.chf("0.81"));
        assertThat(Geld.chf("5.00").negiert()).isEqualTo(Geld.chf("-5.00"));
    }

    @Test
    void verweigertVerschiedeneWaehrungen() {
        assertThatThrownBy(() -> Geld.chf("1.00").plus(Geld.eur("1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CHF")
                .hasMessageContaining("EUR");
        assertThatThrownBy(() -> Geld.chf("1.00").compareTo(Geld.eur("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "1.02, 1.00",
        "1.024, 1.00",
        "1.025, 1.05",
        "1.03, 1.05",
        "1.07, 1.05",
        "1.075, 1.10",
        "1.08, 1.10",
        "99.99, 100.00",
        "0.00, 0.00",
        "-1.03, -1.05",
    })
    void rundetAufFuenfRappen(String betrag, String erwartet) {
        assertThat(Geld.chf(betrag).aufFuenfRappenGerundet()).isEqualTo(Geld.chf(erwartet));
    }

    @Test
    void vergleichtUndPrueftVorzeichen() {
        var zehn = Geld.chf("10.00");
        assertThat(zehn.istGroesserAls(Geld.chf("9.99"))).isTrue();
        assertThat(zehn.istKleinerAls(Geld.chf("10.01"))).isTrue();
        assertThat(zehn.istPositiv()).isTrue();
        assertThat(zehn.istNegativ()).isFalse();
        assertThat(Geld.nullBetrag(Geld.CHF).istNull()).isTrue();
        assertThat(Geld.chf("-0.01").istNegativ()).isTrue();
    }

    @Test
    void zeigtWaehrungUndBetrag() {
        assertThat(Geld.chf("1840")).hasToString("CHF 1840.00");
        assertThat(Geld.eur("420.5")).hasToString("EUR 420.50");
    }
}
