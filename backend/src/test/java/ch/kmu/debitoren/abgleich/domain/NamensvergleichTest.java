package ch.kmu.debitoren.abgleich.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NamensvergleichTest {

    @ParameterizedTest(name = "«{0}» und «{1}» passen: {2}")
    @CsvSource(delimiter = '|', textBlock = """
            Gasser Treuhand GmbH  | GASSER TREUHAND          | true
            Keller Haustechnik AG | Keller Haustechnik       | true
            Brunner Marco         | Marco Brunner            | true
            Brunner Marco         | M. Brunner               | true
            Müller & Co. AG       | MUELLER UND CO           | true
            Café Zürich Sàrl      | Cafe Zuerich             | true
            Lüthi Bau AG          | Lüthi Bau Holding AG     | true
            Brunner Marco         | Brunner Anna             | false
            Keller Haustechnik AG | Keller Gartenbau AG      | false
            Gasser Treuhand GmbH  | GmbH                     | false
            Brunner Marco         | M. B.                    | false
            Keller Haustechnik AG | ''                       | false
            """)
    void vergleichtWoerterStattSchreibweise(String rechnung, String auftraggeber, boolean passen) {
        assertThat(Namensvergleich.passen(rechnung, auftraggeber)).isEqualTo(passen);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Gasser Treuhand GmbH  | gasser treuhand
            Herr Marco Brunner    | marco brunner
            """)
    void entferntRechtsformenUndAnreden(String name, String woerter) {
        assertThat(String.join(" ", Namensvergleich.woerter(name))).isEqualTo(woerter);
    }
}
