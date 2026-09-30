package ch.kmu.debitoren.shared.mandant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regeln der Mandantenwahl; der Weg durch die Filterkette steht in SicherheitWebTest. */
class MandantFilterTest {

    private static final UUID A = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final UUID B = UUID.fromString("019a0000-0000-7000-8000-000000000002");

    @Test
    void ohneHeaderGiltDerEinzigeMandant() {
        assertThat(MandantFilter.waehlen(null, List.of(A))).contains(A);
        assertThat(MandantFilter.waehlen(" ", List.of(A))).contains(A);
    }

    @Test
    void ohneHeaderBleibtDerMandantBeiMehrerenOffen() {
        assertThat(MandantFilter.waehlen(null, List.of(A, B))).isEmpty();
        assertThat(MandantFilter.waehlen(null, List.of())).isEmpty();
    }

    @Test
    void headerWaehltEinenErlaubtenMandanten() {
        assertThat(MandantFilter.waehlen(B.toString(), List.of(A, B))).contains(B);
    }

    @Test
    void headerMitFremdemMandantenWirdAbgelehnt() {
        assertThatThrownBy(() -> MandantFilter.waehlen(B.toString(), List.of(A)))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(AllgemeinerFehlercode.MANDANT_NICHT_ERLAUBT));
    }

    @Test
    void ungueltigerHeaderWirdAbgelehnt() {
        assertThatThrownBy(() -> MandantFilter.waehlen("abc", List.of(A)))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(AllgemeinerFehlercode.MANDANT_UNGUELTIG));
    }
}
