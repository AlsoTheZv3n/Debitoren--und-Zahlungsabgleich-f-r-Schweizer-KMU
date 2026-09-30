package ch.kmu.debitoren.reporting.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Currency;

/**
 * Zahlungseingänge eines Monats nach Valuta.
 *
 * @param automatisch davon, was der Abgleich ohne Zutun einer Rechnung zugeordnet hat
 */
public record Monatseingang(
        YearMonth monat, Currency waehrung, BigDecimal betrag, BigDecimal automatisch, long anzahl) {}
