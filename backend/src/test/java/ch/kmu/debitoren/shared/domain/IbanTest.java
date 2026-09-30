package ch.kmu.debitoren.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IbanTest {

    @Test
    void normalisiertLeerzeichenUndKleinschreibung() {
        var iban = new Iban("ch93 0076 2011 6238 5295 7");
        assertThat(iban.wert()).isEqualTo("CH9300762011623852957");
        assertThat(iban.formatiert()).isEqualTo("CH93 0076 2011 6238 5295 7");
    }

    @Test
    void erkenntQrIbanAnDerInstitutsidentifikation() {
        assertThat(new Iban("CH4431999123000889012").istQrIban()).isTrue();
        assertThat(new Iban("CH5800791123000889012").istQrIban()).isFalse();
        assertThat(new Iban("CH9300762011623852957").istQrIban()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "CH9300762011623852958", // falsche Prüfziffer
                "DE89370400440532013000", // keine Schweizer IBAN
                "CH930076201162385295", // zu kurz
                "CH93-0076-2011-6238-5295-7",
            })
    void verweigertUngueltigeIban(String wert) {
        assertThatThrownBy(() -> new Iban(wert))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(AllgemeinerFehlercode.UNGUELTIGER_WERT));
    }

    @Test
    void verlangtEinenWert() {
        assertThatThrownBy(() -> new Iban(null)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void speichertOhneLeerzeichen() {
        var konverter = new IbanConverter();
        assertThat(konverter.convertToDatabaseColumn(new Iban("CH93 0076 2011 6238 5295 7")))
                .isEqualTo("CH9300762011623852957");
        assertThat(konverter.convertToEntityAttribute("CH9300762011623852957"))
                .isEqualTo(new Iban("CH9300762011623852957"));
        assertThat(konverter.convertToDatabaseColumn(null)).isNull();
        assertThat(konverter.convertToEntityAttribute(null)).isNull();
    }
}
