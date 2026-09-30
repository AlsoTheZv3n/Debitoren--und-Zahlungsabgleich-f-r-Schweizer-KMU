package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.rechnung.application.Entwurf;
import ch.kmu.debitoren.rechnung.domain.MwstCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Typische Leistungen je Demo-Mandant, damit die Testrechnungen plausibel aussehen. */
final class Leistungskatalog {

    private enum Mengenart {
        STUNDEN,
        STUECK,
        LAUFMETER,
        PAUSCHAL
    }

    private record Leistung(
            String beschreibung, String einheit, int preisVon, int preisBis, MwstCode code, Mengenart menge) {}

    private static final List<Leistung> WERKSTATT = List.of(
            new Leistung("Arbeitsstunde Metallbauer", "Std", 95, 125, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Arbeitsstunde Lernende", "Std", 45, 60, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Stahlprofil verzinkt", "m", 18, 42, MwstCode.NORMAL, Mengenart.LAUFMETER),
            new Leistung("Chromstahlblech geschliffen", "m²", 85, 160, MwstCode.NORMAL, Mengenart.STUECK),
            new Leistung("Pulverbeschichtung", "Stk", 30, 120, MwstCode.NORMAL, Mengenart.STUECK),
            new Leistung("Kleinmaterial und Befestigungen", null, 15, 80, MwstCode.NORMAL, Mengenart.PAUSCHAL),
            new Leistung("Anfahrt Region Bern", null, 45, 90, MwstCode.NORMAL, Mengenart.PAUSCHAL),
            new Leistung("Reparatur Gartentor", null, 180, 650, MwstCode.NORMAL, Mengenart.PAUSCHAL),
            new Leistung("Beratung und Aufmass", "Std", 110, 140, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Übernachtung Montageteam", "Nacht", 110, 160, MwstCode.BEHERBERGUNG, Mengenart.STUECK));

    private static final List<Leistung> GARTENBAU = List.of(
            new Leistung("Rasenpflege", "Std", 72, 88, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Heckenschnitt", "Std", 72, 92, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Stauden und Sträucher", "Stk", 8, 45, MwstCode.REDUZIERT, Mengenart.STUECK),
            new Leistung("Obstbaum Hochstamm", "Stk", 120, 260, MwstCode.REDUZIERT, Mengenart.STUECK),
            new Leistung("Rasensamen Schattenmischung", "kg", 18, 32, MwstCode.REDUZIERT, Mengenart.STUECK),
            new Leistung("Rindenmulch", "m³", 60, 95, MwstCode.NORMAL, Mengenart.STUECK),
            new Leistung("Grüngutentsorgung", null, 40, 150, MwstCode.NORMAL, Mengenart.PAUSCHAL),
            new Leistung("Gartenplanung", "Std", 110, 135, MwstCode.NORMAL, Mengenart.STUNDEN),
            new Leistung("Maschinenmiete Minibagger", "Tag", 280, 420, MwstCode.NORMAL, Mengenart.STUECK),
            new Leistung("Gebühr Grüngutsammelstelle", null, 20, 60, MwstCode.OHNE, Mengenart.PAUSCHAL));

    private final List<Leistung> leistungen;

    private Leistungskatalog(List<Leistung> leistungen) {
        this.leistungen = leistungen;
    }

    static Leistungskatalog fuer(UUID mandantId) {
        return new Leistungskatalog(DemoMandanten.SEELAND_GARTENBAU.equals(mandantId) ? GARTENBAU : WERKSTATT);
    }

    /** Eine Position; Leistungen am Ende der Liste kommen seltener vor. */
    Entwurf.Position position(Random zufall) {
        int index = Math.min(leistungen.size() - 1, (int) Math.abs(zufall.nextGaussian() * leistungen.size() / 2.5));
        var leistung = leistungen.get(index);
        return new Entwurf.Position(
                leistung.beschreibung(),
                menge(leistung.menge(), zufall),
                leistung.einheit(),
                preis(leistung, zufall),
                leistung.code());
    }

    private static BigDecimal menge(Mengenart art, Random zufall) {
        return switch (art) {
            case STUNDEN -> BigDecimal.valueOf(2 + zufall.nextInt(63)).divide(BigDecimal.valueOf(4));
            case STUECK -> BigDecimal.valueOf(1 + zufall.nextInt(24));
            case LAUFMETER -> BigDecimal.valueOf(1 + zufall.nextInt(79)).divide(BigDecimal.valueOf(2));
            case PAUSCHAL -> BigDecimal.ONE;
        };
    }

    /** Preise auf 5 Rappen, wie sie ein Betrieb kalkulieren würde. */
    private static BigDecimal preis(Leistung leistung, Random zufall) {
        double preis = leistung.preisVon() + zufall.nextDouble() * (leistung.preisBis() - leistung.preisVon());
        return BigDecimal.valueOf(Math.round(preis * 20)).divide(BigDecimal.valueOf(20), 2, RoundingMode.UNNECESSARY);
    }
}
