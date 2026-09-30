package ch.kmu.debitoren.reporting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.reporting.domain.Altersstruktur.Band;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Aenderung;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Aktion;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wie Zeilen der _aud-Tabellen im Log erscheinen, ohne Datenbank. */
class AuditobjektTest {

    private static final Auditobjekt RECHNUNG = Auditobjekt.von(Objektart.RECHNUNG);

    @Test
    void aenderungZeigtNurUnterschiedlicheFelderLesbar() {
        var vorher = rechnung("OFFEN", "464.55", "0.00");
        var nachher = rechnung("TEILBEZAHLT", "464.55", "20.00");

        assertThat(JdbcAuditLog.aenderungen(RECHNUNG, Aktion.GEAENDERT, vorher, nachher))
                .containsExactly(
                        new Aenderung("Status", "Offen", "Teilbezahlt"),
                        new Aenderung("Mahngebühren", "CHF 0.00", "CHF 20.00"));
    }

    @Test
    void anlegenUndLoeschenZeigenDieWichtigstenFelder() {
        var zeile = rechnung("ENTWURF", "1840.00", "0.00");
        assertThat(JdbcAuditLog.aenderungen(RECHNUNG, Aktion.ANGELEGT, null, zeile))
                .containsExactly(
                        new Aenderung("Status", null, "Entwurf"),
                        new Aenderung("Kunde", null, "Keller Haustechnik AG"),
                        new Aenderung("Rechnungsdatum", null, "01.09.2026"),
                        new Aenderung("Total", null, "CHF 1’840.00"));
        // Ohne früheren Stand, etwa bei Objekten von vor dem Log, wie beim Anlegen.
        assertThat(JdbcAuditLog.aenderungen(RECHNUNG, Aktion.GEAENDERT, null, zeile))
                .hasSize(4);
        assertThat(JdbcAuditLog.aenderungen(RECHNUNG, Aktion.GELOESCHT, null, zeile))
                .first()
                .isEqualTo(new Aenderung("Status", "Entwurf", null));
    }

    @Test
    void bezeichnungenUndWerte() {
        assertThat(RECHNUNG.bezeichnung().apply(rechnung("ENTWURF", "1.00", "0.00")))
                .isEqualTo("Entwurf · Keller Haustechnik AG");
        var satz = Map.<String, Object>of(
                "bezeichnung", "Normalsatz", "satz", new BigDecimal("8.10"), "gueltig_ab", Date.valueOf("2024-01-01"));
        assertThat(Auditobjekt.von(Objektart.MWST_SATZ).bezeichnung().apply(satz))
                .isEqualTo("Normalsatz ab 01.01.2024");
        assertThat(Auditobjekt.Wert.PROZENT.lesbar(new BigDecimal("8.10"), satz))
                .isEqualTo("8.1 %");
        assertThat(Auditobjekt.Wert.JA_NEIN.lesbar(true, satz)).isEqualTo("ja");
        assertThat(Auditobjekt.Wert.ZEITPUNKT.lesbar(Timestamp.from(Instant.parse("2026-09-30T06:15:00Z")), satz))
                .isEqualTo("30.09.2026 08:15");
        assertThat(Auditobjekt.Wert.DATUM.lesbar(LocalDate.of(2026, 9, 30), satz))
                .isEqualTo("30.09.2026");
        assertThat(Auditobjekt.Wert.TEXT.lesbar(" ", satz)).isNull();
        assertThat(Auditobjekt.Wert.CODE.lesbar("UNBEKANNT", satz)).isEqualTo("UNBEKANNT");
    }

    @Test
    void altersbaender() {
        assertThat(Band.fuer(-3)).isEqualTo(Band.NICHT_FAELLIG);
        assertThat(Band.fuer(0)).isEqualTo(Band.NICHT_FAELLIG);
        assertThat(Band.fuer(1)).isEqualTo(Band.TAGE_1_30);
        assertThat(Band.fuer(30)).isEqualTo(Band.TAGE_1_30);
        assertThat(Band.fuer(31)).isEqualTo(Band.TAGE_31_60);
        assertThat(Band.fuer(61)).isEqualTo(Band.TAGE_61_90);
        assertThat(Band.fuer(91)).isEqualTo(Band.UEBER_90);
    }

    private static Map<String, Object> rechnung(String status, String total, String gebuehren) {
        var zeile = new HashMap<String, Object>();
        zeile.put("status", status);
        zeile.put("rechnungsnummer", status.equals("ENTWURF") ? null : "RE-2026-0412");
        zeile.put("empfaenger_name", "Keller Haustechnik AG");
        zeile.put("rechnungsdatum", Date.valueOf("2026-09-01"));
        zeile.put("waehrung", "CHF");
        zeile.put("total", new BigDecimal(total));
        zeile.put("mahngebuehren", new BigDecimal(gebuehren));
        return zeile;
    }
}
