package ch.kmu.debitoren.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/** Eigenschaften, die für alle Beträge gelten müssen, geprüft mit zufälligen Werten. */
class GeldEigenschaftenTest {

    private static final BigDecimal FUENF_RAPPEN = new BigDecimal("0.05");
    private static final BigDecimal HALBE_FUENF_RAPPEN = new BigDecimal("0.025");

    @Property
    void plusUndMinusHebenSichAuf(@ForAll("betraege") BigDecimal a, @ForAll("betraege") BigDecimal b) {
        var x = new Geld(a, Geld.CHF);
        var y = new Geld(b, Geld.CHF);
        assertThat(x.plus(y).minus(y)).isEqualTo(x);
    }

    @Property
    void addierenIstKommutativ(@ForAll("betraege") BigDecimal a, @ForAll("betraege") BigDecimal b) {
        var x = new Geld(a, Geld.CHF);
        var y = new Geld(b, Geld.CHF);
        assertThat(x.plus(y)).isEqualTo(y.plus(x));
    }

    @Property
    void fuenfRappenRundungTrifftVielfachesVonFuenfRappen(@ForAll("betraege") BigDecimal betrag) {
        var gerundet = new Geld(betrag, Geld.CHF).aufFuenfRappenGerundet();
        assertThat(gerundet.betrag().remainder(FUENF_RAPPEN)).isEqualByComparingTo("0");
    }

    @Property
    void fuenfRappenRundungWeichtHoechstensEinenHalbenSchrittAb(@ForAll("betraege") BigDecimal betrag) {
        var geld = new Geld(betrag, Geld.CHF);
        var abweichung = geld.aufFuenfRappenGerundet().minus(geld).betrag().abs();
        assertThat(abweichung).isLessThanOrEqualTo(HALBE_FUENF_RAPPEN);
    }

    @Property
    void fuenfRappenRundungIstIdempotent(@ForAll("betraege") BigDecimal betrag) {
        var einmal = new Geld(betrag, Geld.CHF).aufFuenfRappenGerundet();
        assertThat(einmal.aufFuenfRappenGerundet()).isEqualTo(einmal);
    }

    @Provide
    Arbitrary<BigDecimal> betraege() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-10000000"), new BigDecimal("10000000"))
                .ofScale(2);
    }
}
