package ch.kmu.debitoren.rechnung;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.kmu.debitoren.ApiTestBasis;
import ch.kmu.debitoren.rechnung.application.Entwurf;
import ch.kmu.debitoren.rechnung.application.RechnungVerwaltung;
import ch.kmu.debitoren.rechnung.domain.MwstCode;
import ch.kmu.debitoren.shared.dokumente.Dokumentablage;
import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.stammdaten.application.KundeDaten;
import ch.kmu.debitoren.stammdaten.application.KundenVerwaltung;
import ch.kmu.debitoren.stammdaten.application.MandantVerwaltung;
import ch.kmu.debitoren.stammdaten.application.NeuerMandant;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

/** Ereignisse zwischen den Modulen, geprüft mit dem Scenario von Spring Modulith. */
@EnableScenarios
class EreignisseIT extends ApiTestBasis {

    @Autowired
    MandantVerwaltung mandantVerwaltung;

    @Autowired
    KundenVerwaltung kundenVerwaltung;

    @Autowired
    RechnungVerwaltung rechnungVerwaltung;

    @Autowired
    Dokumentablage ablage;

    @Test
    void neuerMandantErhaeltDieGesetzlichenMwstSaetze(Scenario scenario) {
        var id = Ids.neu();
        var neu = new NeuerMandant(
                id, "Neue Firma AG", Adresse.schweiz("Weg", "1", "3000", "Bern"), new Iban(IBAN), null, null, null);

        scenario.stimulate(() -> Systemkontext.ausfuehren(() -> mandantVerwaltung.anlegen(neu)))
                .andWaitForStateChange(() -> saetze(id), anzahl -> anzahl > 0)
                .andVerify(anzahl -> assertThat(anzahl).isEqualTo(7));
    }

    @Test
    void freigabeLegtDasPdfAb(Scenario scenario) {
        var mandant = mandantMitQrIban("PDF Test AG");
        Systemkontext.fuerMandant(mandant, () -> {
            var kunde = kundenVerwaltung.anlegen(
                    new KundeDaten(null, "Keller AG", Adresse.schweiz("Weg", "2", "3000", "Bern"), null, 30));
            var entwurf = rechnungVerwaltung.entwurfAnlegen(new Entwurf(
                    kunde.getId(),
                    LocalDate.of(2026, 9, 1),
                    null,
                    Geld.CHF,
                    null,
                    List.of(new Entwurf.Position(
                            "Montage", BigDecimal.ONE, "Std", new BigDecimal("100.00"), MwstCode.NORMAL))));

            scenario.stimulate(() -> rechnungVerwaltung.freigeben(entwurf.getId(), entwurf.getVersion()))
                    .andWaitForStateChange(() -> pdfSchluessel(entwurf.getId()), schluessel -> schluessel != null)
                    .andVerify(schluessel -> {
                        assertThat(schluessel).isEqualTo("rechnungen/%s/%s.pdf".formatted(mandant, entwurf.getId()));
                        assertThat(ablage.lesen(schluessel)).isPresent();
                    });
            // Die Registry vermerkt die Zustellung kurz nach dem Commit des Empfängers und löscht das Ereignis dann.
            await().atMost(Duration.ofSeconds(10)).until(() -> offeneEreignisse(entwurf.getId()) == 0);
        });
    }

    private long saetze(UUID mandant) {
        return jdbc.queryForObject("select count(*) from mwst_satz where mandant_id = ?", Long.class, mandant);
    }

    private String pdfSchluessel(UUID rechnung) {
        return jdbc.queryForObject("select pdf_schluessel from rechnung where id = ?", String.class, rechnung);
    }

    private long offeneEreignisse(UUID rechnung) {
        return jdbc.queryForObject(
                "select count(*) from event_publication where serialized_event like ?",
                Long.class,
                "%" + rechnung + "%");
    }
}
