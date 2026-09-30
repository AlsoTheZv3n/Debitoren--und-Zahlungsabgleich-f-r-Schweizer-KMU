package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Rechenregeln einer Rechnung. Einzelpreise sind ohne MWST; die MWST wird je Satz auf die Summe der
 * Nettobeträge gerechnet und auf Rappen gerundet. Bei CHF wird das Total auf 5 Rappen gerundet, sofern
 * der Mandant das nicht abgeschaltet hat.
 */
public final class Rechnungsrechner {

    private Rechnungsrechner() {}

    /** Ein Posten für die Berechnung: Nettobetrag und MWST-Satz. */
    public record Posten(BigDecimal betrag, MwstCode code, BigDecimal prozent) {}

    public static BigDecimal positionsbetrag(BigDecimal menge, BigDecimal einzelpreis) {
        return menge.multiply(einzelpreis).setScale(2, RoundingMode.HALF_UP);
    }

    public static Summen berechnen(List<Posten> posten, Currency waehrung, boolean rundungFuenfRappen) {
        var basisJeSatz = new LinkedHashMap<Satzschluessel, Geld>();
        for (var einzel : posten) {
            basisJeSatz.merge(
                    new Satzschluessel(einzel.code(), einzel.prozent().setScale(2, RoundingMode.UNNECESSARY)),
                    new Geld(einzel.betrag(), waehrung),
                    Geld::plus);
        }
        var anteile = basisJeSatz.entrySet().stream()
                .map(eintrag -> new Summen.MwstAnteil(
                        eintrag.getKey().code(),
                        eintrag.getKey().prozent(),
                        eintrag.getValue(),
                        eintrag.getValue().mal(eintrag.getKey().prozent().movePointLeft(2))))
                .sorted(Comparator.comparing((Summen.MwstAnteil anteil) -> anteil.prozent())
                        .reversed()
                        .thenComparing(Summen.MwstAnteil::code))
                .toList();

        var nichts = Geld.nullBetrag(waehrung);
        var netto = anteile.stream().map(Summen.MwstAnteil::basis).reduce(nichts, Geld::plus);
        var mwst = anteile.stream().map(Summen.MwstAnteil::betrag).reduce(nichts, Geld::plus);
        var brutto = netto.plus(mwst);
        var total = rundungFuenfRappen && Geld.CHF.equals(waehrung) ? brutto.aufFuenfRappenGerundet() : brutto;
        return new Summen(netto, anteile, mwst, total.minus(brutto), total);
    }

    private record Satzschluessel(MwstCode code, BigDecimal prozent) {}
}
