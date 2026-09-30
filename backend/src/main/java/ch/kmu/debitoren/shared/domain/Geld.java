package ch.kmu.debitoren.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Betrag mit Währung, immer mit zwei Nachkommastellen und kaufmännisch gerundet ({@code HALF_UP}).
 * Für Beträge kommt {@code double} nie vor.
 */
public record Geld(BigDecimal betrag, Currency waehrung) implements Comparable<Geld> {

    public static final Currency CHF = Currency.getInstance("CHF");
    public static final Currency EUR = Currency.getInstance("EUR");

    private static final int SKALA = 2;
    private static final RoundingMode RUNDUNG = RoundingMode.HALF_UP;
    private static final BigDecimal ZWANZIG = BigDecimal.valueOf(20);

    public Geld {
        Objects.requireNonNull(betrag, "betrag");
        Objects.requireNonNull(waehrung, "waehrung");
        betrag = betrag.setScale(SKALA, RUNDUNG);
    }

    public static Geld von(String betrag, Currency waehrung) {
        return new Geld(new BigDecimal(betrag), waehrung);
    }

    public static Geld chf(String betrag) {
        return von(betrag, CHF);
    }

    public static Geld eur(String betrag) {
        return von(betrag, EUR);
    }

    public static Geld nullBetrag(Currency waehrung) {
        return new Geld(BigDecimal.ZERO, waehrung);
    }

    public Geld plus(Geld andere) {
        pruefeGleicheWaehrung(andere);
        return new Geld(betrag.add(andere.betrag), waehrung);
    }

    public Geld minus(Geld andere) {
        pruefeGleicheWaehrung(andere);
        return new Geld(betrag.subtract(andere.betrag), waehrung);
    }

    public Geld mal(BigDecimal faktor) {
        return new Geld(betrag.multiply(faktor), waehrung);
    }

    public Geld negiert() {
        return new Geld(betrag.negate(), waehrung);
    }

    /** Rundet auf 5 Rappen (0.05), wie es bei Rechnungen in CHF üblich ist. */
    public Geld aufFuenfRappenGerundet() {
        var zwanzigstel = betrag.multiply(ZWANZIG).setScale(0, RUNDUNG);
        return new Geld(zwanzigstel.divide(ZWANZIG), waehrung);
    }

    public boolean istNull() {
        return betrag.signum() == 0;
    }

    public boolean istPositiv() {
        return betrag.signum() > 0;
    }

    public boolean istNegativ() {
        return betrag.signum() < 0;
    }

    public boolean istGroesserAls(Geld andere) {
        return compareTo(andere) > 0;
    }

    public boolean istKleinerAls(Geld andere) {
        return compareTo(andere) < 0;
    }

    @Override
    public int compareTo(Geld andere) {
        pruefeGleicheWaehrung(andere);
        return betrag.compareTo(andere.betrag);
    }

    private void pruefeGleicheWaehrung(Geld andere) {
        Objects.requireNonNull(andere, "andere");
        if (!waehrung.equals(andere.waehrung)) {
            throw new IllegalArgumentException(
                    "Beträge in %s und %s lassen sich nicht verrechnen".formatted(waehrung, andere.waehrung));
        }
    }

    @Override
    public String toString() {
        return waehrung.getCurrencyCode() + " " + betrag.toPlainString();
    }
}
