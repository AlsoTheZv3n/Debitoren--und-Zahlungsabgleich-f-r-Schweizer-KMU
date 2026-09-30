package ch.kmu.debitoren.rechnung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import net.codecrete.qrbill.generator.Payments;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.constraints.UpperChars;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScorReferenzTest {

    @Test
    void akzeptiertDasBeispielAusIso11649() {
        var referenz = new ScorReferenz("rf18 5390 0754 7034");
        assertThat(referenz.wert()).isEqualTo("RF18539007547034");
        assertThat(referenz.formatiert()).isEqualTo("RF18 5390 0754 7034");
        assertThat(referenz.art()).isEqualTo(Referenzart.SCOR);
    }

    @Test
    void berechnetDiePruefziffern() {
        assertThat(ScorReferenz.aus("539007547034").wert()).isEqualTo("RF18539007547034");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RF19539007547034", "RF18", "XX18539007547034", "RF18539007547034ABCDEFGHIJKL"})
    void verweigertUngueltigeReferenzen(String wert) {
        assertThatThrownBy(() -> new ScorReferenz(wert)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void verweigertUngueltigeGrundlagen() {
        assertThatThrownBy(() -> ScorReferenz.aus("")).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> ScorReferenz.aus("A".repeat(22))).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> ScorReferenz.aus("12-34")).isInstanceOf(FachlicherFehler.class);
    }

    @Property
    void stimmtMitDerReferenzimplementierungUeberein(
            @ForAll @NumericChars @UpperChars @StringLength(min = 1, max = 21) String grundlage) {
        var referenz = ScorReferenz.aus(grundlage);
        assertThat(Payments.isValidISO11649Reference(referenz.wert())).isTrue();
        assertThat(referenz.wert()).isEqualTo(Payments.createISO11649Reference(grundlage));
    }

    @Property
    void unterscheidetGrossUndKleinschreibungNicht(@ForAll @AlphaChars @StringLength(min = 1, max = 21) String text) {
        assertThat(ScorReferenz.aus(text.toLowerCase())).isEqualTo(ScorReferenz.aus(text.toUpperCase()));
    }

    @Test
    void erkenntDieArtAmPraefix() {
        assertThat(Zahlungsreferenz.lesen("RF18 5390 0754 7034")).isInstanceOf(ScorReferenz.class);
        assertThat(Zahlungsreferenz.lesen("210000000003139471430009017")).isInstanceOf(QrReferenz.class);
        var konverter = new ZahlungsreferenzConverter();
        assertThat(konverter.convertToDatabaseColumn(ScorReferenz.aus("A1"))).startsWith("RF");
        assertThat(konverter.convertToEntityAttribute("RF18539007547034")).isEqualTo(ScorReferenz.aus("539007547034"));
        assertThat(konverter.convertToDatabaseColumn(null)).isNull();
        assertThat(konverter.convertToEntityAttribute(null)).isNull();
    }
}
