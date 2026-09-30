package ch.kmu.debitoren.stammdaten.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.TestcontainersConfiguration;
import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.mandant.MandantTenantResolver;
import ch.kmu.debitoren.stammdaten.domain.Kunde;
import ch.kmu.debitoren.stammdaten.domain.KundeRepository;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Migrationen, Constraints und Mandantentrennung gegen echtes PostgreSQL. Die Tests steuern Transaktionen
 * selbst, weil Hibernate den Mandanten beim Öffnen der Session liest.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, MandantTenantResolver.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MandantentrennungIT {

    @Autowired
    KundeRepository kunden;

    @Autowired
    MandantRepository mandanten;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transaktionen;

    private TransactionTemplate tx;
    private UUID mandantA;
    private UUID mandantB;

    @BeforeEach
    void mandantenAnlegen() {
        tx = new TransactionTemplate(transaktionen);
        mandantA = mandantEinfuegen("Mandant A");
        mandantB = mandantEinfuegen("Mandant B");
    }

    @Test
    void kundeErhaeltDenMandantenAusDemKontext() {
        speichernFuer(mandantA, kunde("K-1"));

        var mandantId = jdbc.queryForObject(
                "select mandant_id from kunde where kundennummer = 'K-1' and mandant_id = ?", UUID.class, mandantA);
        assertThat(mandantId).isEqualTo(mandantA);
    }

    @Test
    void kundeIstNurImEigenenMandantenSichtbar() {
        speichernFuer(mandantA, kunde("K-2"));

        long inA = MandantKontext.berechnenFuer(mandantA, () -> tx.execute(s -> kunden.anzahl()));
        long inB = MandantKontext.berechnenFuer(mandantB, () -> tx.execute(s -> kunden.anzahl()));
        Optional<Kunde> ausB =
                MandantKontext.berechnenFuer(mandantB, () -> tx.execute(s -> kunden.findeNachKundennummer("K-2")));

        assertThat(inA).isEqualTo(1L);
        assertThat(inB).isZero();
        assertThat(ausB).isEmpty();
    }

    @Test
    void ohneMandantIstNichtsSichtbar() {
        speichernFuer(mandantA, kunde("K-3"));

        long ohneMandant = tx.execute(s -> kunden.anzahl());
        assertThat(ohneMandant).isZero();
    }

    @Test
    void ohneMandantScheitertDasSpeichern() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> kunden.speichern(kunde("K-4"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void kundennummerIstJeMandantEindeutig() {
        speichernFuer(mandantA, kunde("K-5"));
        speichernFuer(mandantB, kunde("K-5"));

        assertThatThrownBy(() -> speichernFuer(mandantA, kunde("K-5")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mandantenWerdenNachNameSortiertGeladen() {
        var gefunden = tx.execute(s -> mandanten.findeAlle(List.of(mandantB, mandantA)));

        assertThat(gefunden).extracting(m -> m.getName()).containsExactly("Mandant A", "Mandant B");
    }

    @Test
    void datenbankPrueftIbanUndQrIban() {
        assertThatThrownBy(() -> jdbc.update(
                        "insert into mandant (id, name, plz, ort, iban) values (?, 'X', '3000', 'Bern', 'CH00')",
                        Ids.neu()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "insert into mandant (id, name, plz, ort, iban) values (?, 'X', '3000', 'Bern', ?)",
                        Ids.neu(),
                        "CH4431999123000889012"))
                .as("QR-IBAN im Feld IBAN")
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "insert into mandant (id, name, plz, ort, iban, qr_iban) values (?, 'X', '3000', 'Bern', ?, ?)",
                        Ids.neu(),
                        "CH9300762011623852957",
                        "CH9300762011623852957"))
                .as("normale IBAN im Feld QR-IBAN")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void speichernFuer(UUID mandant, Kunde kunde) {
        MandantKontext.ausfuehrenFuer(mandant, () -> tx.executeWithoutResult(s -> kunden.speichern(kunde)));
    }

    private UUID mandantEinfuegen(String name) {
        var id = Ids.neu();
        jdbc.update(
                "insert into mandant (id, name, plz, ort, iban) values (?, ?, '3000', 'Bern', 'CH9300762011623852957')",
                id,
                name);
        return id;
    }

    private static Kunde kunde(String kundennummer) {
        return new Kunde(kundennummer, "Kunde " + kundennummer, Adresse.schweiz("Weg", "1", "3000", "Bern"), null, 30);
    }
}
