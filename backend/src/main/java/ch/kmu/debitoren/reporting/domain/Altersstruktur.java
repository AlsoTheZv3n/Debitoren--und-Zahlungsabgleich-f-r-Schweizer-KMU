package ch.kmu.debitoren.reporting.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

/** Offene Posten nach Tagen über der Fälligkeit, je Währung; jede Währung hat alle fünf Bänder. */
public record Altersstruktur(LocalDate stichtag, List<Waehrung> waehrungen) {

    public enum Band {
        NICHT_FAELLIG,
        TAGE_1_30,
        TAGE_31_60,
        TAGE_61_90,
        UEBER_90;

        /** Band für so viele Tage über der Fälligkeit; 0 und weniger heisst noch nicht fällig. */
        public static Band fuer(long tageUeberFaelligkeit) {
            if (tageUeberFaelligkeit <= 0) {
                return NICHT_FAELLIG;
            }
            if (tageUeberFaelligkeit <= 30) {
                return TAGE_1_30;
            }
            if (tageUeberFaelligkeit <= 60) {
                return TAGE_31_60;
            }
            return tageUeberFaelligkeit <= 90 ? TAGE_61_90 : UEBER_90;
        }
    }

    public record Altersband(Band band, BigDecimal betrag, long anzahl) {}

    public record Waehrung(Currency waehrung, List<Altersband> baender, BigDecimal total) {}
}
