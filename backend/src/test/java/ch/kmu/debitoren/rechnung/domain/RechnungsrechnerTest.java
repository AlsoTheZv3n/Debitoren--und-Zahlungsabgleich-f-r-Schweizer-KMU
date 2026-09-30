package ch.kmu.debitoren.rechnung.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.rechnung.domain.Rechnungsrechner.Posten;
import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

class RechnungsrechnerTest {

    private static final BigDecimal NORMAL = new BigDecimal("8.10");
    private static final BigDecimal REDUZIERT = new BigDecimal("2.60");

    @Test
    void rechnetMwstJeSatzAufDieSummeDerNettobetraege() {
        var summen = Rechnungsrechner.berechnen(
                List.of(
                        new Posten(new BigDecimal("100.00"), MwstCode.NORMAL, NORMAL),
                        new Posten(new BigDecimal("33.33"), MwstCode.NORMAL, NORMAL),
                        new Posten(new BigDecimal("50.00"), MwstCode.REDUZIERT, REDUZIERT)),
                Geld.CHF,
                true);

        assertThat(summen.netto()).isEqualTo(Geld.chf("183.33"));
        assertThat(summen.mwst()).hasSize(2);
        // 133.33 × 8.1 % = 10.79973 → 10.80; 50.00 × 2.6 % = 1.30
        assertThat(summen.mwst().get(0).basis()).isEqualTo(Geld.chf("133.33"));
        assertThat(summen.mwst().get(0).betrag()).isEqualTo(Geld.chf("10.80"));
        assertThat(summen.mwst().get(1).betrag()).isEqualTo(Geld.chf("1.30"));
        assertThat(summen.mwstTotal()).isEqualTo(Geld.chf("12.10"));
        // 195.43 → 195.45
        assertThat(summen.total()).isEqualTo(Geld.chf("195.45"));
        assertThat(summen.rundung()).isEqualTo(Geld.chf("0.02"));
    }

    @Test
    void rundetEuroUndAbgeschalteteRundungNicht() {
        var posten = List.of(new Posten(new BigDecimal("10.01"), MwstCode.OHNE, BigDecimal.ZERO.setScale(2)));

        assertThat(Rechnungsrechner.berechnen(posten, Geld.EUR, true).total()).isEqualTo(Geld.eur("10.01"));
        assertThat(Rechnungsrechner.berechnen(posten, Geld.CHF, false).total()).isEqualTo(Geld.chf("10.01"));
        assertThat(Rechnungsrechner.berechnen(posten, Geld.CHF, true).total()).isEqualTo(Geld.chf("10.00"));
    }

    @Test
    void ohnePostenIstAllesNull() {
        var summen = Rechnungsrechner.berechnen(List.of(), Geld.CHF, true);
        assertThat(summen.total().istNull()).isTrue();
        assertThat(summen.mwst()).isEmpty();
    }

    @Test
    void positionsbetragIstMengeMalPreisAufRappen() {
        assertThat(Rechnungsrechner.positionsbetrag(new BigDecimal("2.333"), new BigDecimal("10.00")))
                .isEqualByComparingTo("23.33");
        assertThat(Rechnungsrechner.positionsbetrag(new BigDecimal("0.125"), new BigDecimal("1.00")))
                .isEqualByComparingTo("0.13");
    }

    @Property
    void totalIstNettoPlusMwstPlusRundung(@ForAll("posten") List<Posten> posten) {
        var summen = Rechnungsrechner.berechnen(posten, Geld.CHF, true);
        assertThat(summen.netto().plus(summen.mwstTotal()).plus(summen.rundung()))
                .isEqualTo(summen.total());
        assertThat(summen.rundung().betrag().abs()).isLessThanOrEqualTo(new BigDecimal("0.02"));
        assertThat(summen.total().betrag().remainder(new BigDecimal("0.05"))).isEqualByComparingTo("0");
    }

    @Property
    void nettoIstDieSummeAllerPosten(@ForAll("posten") List<Posten> posten) {
        var summen = Rechnungsrechner.berechnen(posten, Geld.EUR, true);
        var erwartet = posten.stream().map(Posten::betrag).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(summen.netto().betrag()).isEqualByComparingTo(erwartet);
        assertThat(summen.rundung().istNull()).isTrue();
    }

    @Provide
    Arbitrary<List<Posten>> posten() {
        var betrag = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(2);
        var satz = Arbitraries.of(
                new Posten(BigDecimal.ONE, MwstCode.NORMAL, NORMAL),
                new Posten(BigDecimal.ONE, MwstCode.REDUZIERT, REDUZIERT),
                new Posten(BigDecimal.ONE, MwstCode.BEHERBERGUNG, new BigDecimal("3.80")),
                new Posten(BigDecimal.ONE, MwstCode.OHNE, BigDecimal.ZERO.setScale(2)));
        return net.jqwik.api.Combinators.combine(betrag, satz)
                .as((b, s) -> new Posten(b, s.code(), s.prozent()))
                .list()
                .ofMaxSize(20);
    }
}
