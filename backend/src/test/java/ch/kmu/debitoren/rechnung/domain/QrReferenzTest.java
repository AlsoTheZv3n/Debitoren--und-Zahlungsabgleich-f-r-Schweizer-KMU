package ch.kmu.debitoren.rechnung.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import net.codecrete.qrbill.generator.Payments;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NumericChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QrReferenzTest {

    /** Beispielreferenz aus den Implementation Guidelines von SIX. */
    private static final String SIX_BEISPIEL = "210000000003139471430009017";

    @Test
    void akzeptiertDasBeispielVonSix() {
        var referenz = new QrReferenz("21 00000 00003 13947 14300 09017");
        assertThat(referenz.wert()).isEqualTo(SIX_BEISPIEL);
        assertThat(referenz.formatiert()).isEqualTo("21 00000 00003 13947 14300 09017");
        assertThat(referenz.art()).isEqualTo(Referenzart.QR);
    }

    @Test
    void fuelltLinksMitNullenAufUndHaengtDiePruefzifferAn() {
        var referenz = QrReferenz.aus("2026000412");
        assertThat(referenz.wert()).hasSize(27).startsWith("0000000000000000").contains("2026000412");
        assertThat(Payments.isValidQRReference(referenz.wert())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "210000000003139471430009018", // falsche Prüfziffer
                "21000000000313947143000901", // zu kurz
                "21000000000313947143000901A",
            })
    void verweigertUngueltigeReferenzen(String wert) {
        assertThatThrownBy(() -> new QrReferenz(wert)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void verweigertUngueltigeGrundlagen() {
        assertThatThrownBy(() -> QrReferenz.aus("")).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> QrReferenz.aus("1".repeat(27))).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> QrReferenz.aus("12A")).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void nurNullenSindKeineReferenz() {
        assertThatThrownBy(() -> QrReferenz.aus("0")).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new QrReferenz("0".repeat(27))).isInstanceOf(FachlicherFehler.class);
    }

    @Property
    void stimmtMitDerReferenzimplementierungUeberein(
            @ForAll @NumericChars @StringLength(min = 1, max = 26) String ziffern) {
        Assume.that(!ziffern.matches("0+"));
        var referenz = QrReferenz.aus(ziffern);
        assertThat(Payments.isValidQRReference(referenz.wert())).isTrue();
        assertThat(referenz.wert()).isEqualTo(Payments.createQRReference(ziffern));
    }

    @Property
    void erkenntJedeEinzelneFalscheZiffer(
            @ForAll @NumericChars @StringLength(min = 1, max = 26) String ziffern,
            @ForAll @IntRange(min = 0, max = 26) int stelle,
            @ForAll @IntRange(min = 1, max = 9) int verschiebung) {
        Assume.that(!ziffern.matches("0+"));
        var gueltig = QrReferenz.aus(ziffern).wert();
        var falsch = new StringBuilder(gueltig);
        falsch.setCharAt(stelle, (char) ('0' + (gueltig.charAt(stelle) - '0' + verschiebung) % 10));
        assertThatThrownBy(() -> new QrReferenz(falsch.toString())).isInstanceOf(FachlicherFehler.class);
    }
}
