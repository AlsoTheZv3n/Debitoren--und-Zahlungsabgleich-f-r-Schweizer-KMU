package ch.kmu.debitoren.stammdaten.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import org.junit.jupiter.api.Test;

class KundeTest {

    private static final Adresse ADRESSE = Adresse.schweiz("Industriestrasse", "7", "3052", "Zollikofen");

    @Test
    void erhaeltBeimAnlegenEineId() {
        var kunde = new Kunde("K-10001", "Keller Haustechnik", ADRESSE, " info@keller.example ", 30);
        assertThat(kunde.getId()).isNotNull();
        assertThat(kunde.getId().version()).isEqualTo(7);
        assertThat(kunde.getEmail()).isEqualTo("info@keller.example");
        assertThat(kunde.getMandantId()).as("setzt Hibernate beim Speichern").isNull();
    }

    @Test
    void zahlungsfristLiegtZwischenNullUndEinemJahr() {
        assertThat(new Kunde("K-1", "Bar", ADRESSE, null, 0).getZahlungsfristTage())
                .isZero();
        assertThatThrownBy(() -> new Kunde("K-1", "Bar", ADRESSE, null, 366))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Zahlungsfrist");
        assertThatThrownBy(() -> new Kunde("K-1", "Bar", ADRESSE, null, -1)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void verlangtKundennummerUndName() {
        assertThatThrownBy(() -> new Kunde(" ", "Bar", ADRESSE, null, 30))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Kundennummer");
        assertThatThrownBy(() -> new Kunde("K-1", "", ADRESSE, null, 30))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Name");
    }
}
