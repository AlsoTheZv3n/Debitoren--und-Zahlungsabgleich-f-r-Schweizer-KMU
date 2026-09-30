package ch.kmu.debitoren.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import org.junit.jupiter.api.Test;

class AdresseTest {

    @Test
    void entferntRandleerzeichenUndMachtLeereFelderZuNull() {
        var adresse = new Adresse("  Marktgasse ", " ", "3011", " Bern ", "CH");
        assertThat(adresse.strasse()).isEqualTo("Marktgasse");
        assertThat(adresse.hausnummer()).isNull();
        assertThat(adresse.ort()).isEqualTo("Bern");
    }

    @Test
    void schweizSetztDasLand() {
        assertThat(Adresse.schweiz("Hauptstrasse", "5", "2560", "Nidau").land()).isEqualTo("CH");
    }

    @Test
    void verlangtPlzOrtUndIsoLand() {
        assertThatThrownBy(() -> Adresse.schweiz("Weg", "1", " ", "Bern"))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("PLZ");
        assertThatThrownBy(() -> new Adresse(null, null, "3011", "Bern", "ch"))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("ISO");
    }

    @Test
    void begrenztDieLaengenDesQrZahlteils() {
        assertThatThrownBy(() -> Adresse.schweiz("Weg", "1", "3011", "B".repeat(36)))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("35");
    }
}
