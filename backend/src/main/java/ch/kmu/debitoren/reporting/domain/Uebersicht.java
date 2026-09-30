package ch.kmu.debitoren.reporting.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Kennzahlen für das Dashboard. Beträge stehen je Währung, weil sich Franken und Euro nicht zusammenzählen lassen.
 *
 * @param inKlaerung Zahlungen, die auf eine Zuordnung von Hand warten, mit ihrem Rest
 * @param automatisierung Zahlungen der letzten 30 Tage und wie viele davon der Abgleich selbst zugeordnet hat
 * @param letzterMahnlauf null, solange noch kein Lauf stattgefunden hat
 */
public record Uebersicht(
        LocalDate stichtag,
        List<Summe> offen,
        List<Summe> ueberfaellig,
        List<Summe> inKlaerung,
        long mahnvorschlaege,
        long entwuerfe,
        Automatisierung automatisierung,
        List<Import> letzteImporte,
        Mahnlaufinfo letzterMahnlauf) {

    public record Summe(Currency waehrung, BigDecimal betrag, long anzahl) {}

    public record Automatisierung(long zahlungen, long automatisch) {}

    public record Import(
            UUID id, String dateiname, Instant importiertAm, String importiertVon, int neu, long inKlaerung) {}

    public record Mahnlaufinfo(Instant ausgefuehrtAm, String ausgeloestVon, int vorschlaege) {}
}
