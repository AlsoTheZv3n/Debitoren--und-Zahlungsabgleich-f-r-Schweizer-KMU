package ch.kmu.debitoren.stammdaten.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MandantTest {

    private static final UUID ID = UUID.fromString("019a0000-0000-7000-8000-000000000001");
    private static final Adresse BERN = Adresse.schweiz("Marktgasse", "28", "3011", "Bern");
    private static final Iban IBAN = new Iban("CH5800791123000889012");
    private static final Iban QR_IBAN = new Iban("CH4431999123000889012");

    @Test
    void mitQrIbanGiltDieQrReferenz() {
        var mandant = new Mandant(ID, "Berner Werkstatt AG", BERN, IBAN, QR_IBAN);
        assertThat(mandant.getReferenzart()).isEqualTo(Referenzart.QR);
        assertThat(mandant.getQrIban()).isEqualTo(QR_IBAN);
    }

    @Test
    void ohneQrIbanGiltDieScorReferenz() {
        var mandant = new Mandant(ID, "Seeland Gartenbau GmbH", BERN, new Iban("CH9300762011623852957"), null);
        assertThat(mandant.getReferenzart()).isEqualTo(Referenzart.SCOR);
    }

    @Test
    void qrIbanIstAlsIbanNichtErlaubt() {
        assertThatThrownBy(() -> new Mandant(ID, "Firma", BERN, QR_IBAN, null))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("QR-IBAN");
    }

    @Test
    void normaleIbanIstAlsQrIbanNichtErlaubt() {
        assertThatThrownBy(() -> new Mandant(ID, "Firma", BERN, IBAN, IBAN))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("30000");
    }

    @Test
    void verlangtNameUndAdresse() {
        assertThatThrownBy(() -> new Mandant(ID, " ", BERN, IBAN, null)).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new Mandant(ID, "Firma", null, IBAN, null)).isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> new Mandant(null, "Firma", BERN, IBAN, null)).isInstanceOf(FachlicherFehler.class);
    }
}
