package ch.kmu.debitoren.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.ApiTestBasis;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Envers schreibt jede Änderung mit Benutzer und Mandant in die _aud-Tabellen. */
class AuditErfassungIT extends ApiTestBasis {

    @Test
    @SuppressWarnings("unchecked")
    void freigabeUndStornoStehenMitBenutzerUndMandantImLog() {
        var mandant = mandantMitQrIban("Audit Test AG");
        var buchhaltung = token("buchhaltung", mandant);
        var kunde = kundeAnlegen(buchhaltung, mandant, "Keller Haustechnik AG");
        var entwurf = (Map<String, Object>) client.post()
                .uri("/api/rechnungen")
                .headers(als(buchhaltung, mandant))
                .body(Map.of(
                        "kundeId",
                        kunde,
                        "rechnungsdatum",
                        "2026-09-01",
                        "waehrung",
                        "CHF",
                        "positionen",
                        List.of(Map.of(
                                "beschreibung",
                                "Wartung",
                                "menge",
                                1,
                                "einzelpreis",
                                new BigDecimal("120.00"),
                                "mwstCode",
                                "OHNE"))))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        var id = UUID.fromString((String) entwurf.get("id"));
        var offen = (Map<String, Object>) client.post()
                .uri("/api/rechnungen/{id}/freigabe", id)
                .headers(als(buchhaltung, mandant))
                .body(Map.of("version", entwurf.get("version")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();
        client.post()
                .uri("/api/rechnungen/{id}/storno", id)
                .headers(als(buchhaltung, mandant))
                .body(Map.of("version", offen.get("version"), "grund", "Doppelt erfasst"))
                .exchange()
                .expectStatus()
                .isOk();

        var log = jdbc.queryForList("""
                select a.revtyp, a.status, a.mandant_id, r.benutzer, r.mandant_id as revision_mandant
                from rechnung_aud a join revinfo r on r.id = a.rev
                where a.id = ? order by a.rev
                """, id);
        assertThat(log).extracting(zeile -> zeile.get("status")).containsExactly("ENTWURF", "OFFEN", "STORNIERT");
        assertThat(log)
                .extracting(zeile -> ((Number) zeile.get("revtyp")).intValue())
                .containsExactly(0, 1, 1);
        assertThat(log).allSatisfy(zeile -> {
            assertThat(zeile.get("benutzer")).isEqualTo("buchhaltung");
            assertThat(zeile.get("mandant_id")).isEqualTo(mandant);
            assertThat(zeile.get("revision_mandant")).isEqualTo(mandant);
        });
    }
}
