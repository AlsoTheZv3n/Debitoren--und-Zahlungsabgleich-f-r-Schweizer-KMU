package ch.kmu.debitoren.mahnwesen.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Bestimmt die nächste Mahnstufe einer überfälligen Rechnung. Gemahnt wird Stufe um Stufe, nie übersprungen:
 * Die nächste Stufe folgt, sobald die Rechnung lange genug überfällig ist und die letzte Mahnung weit genug
 * zurückliegt. Nach der höchsten Stufe schlägt der Mahnlauf nichts mehr vor.
 */
public final class Mahnregel {

    private Mahnregel() {}

    public static Optional<Mahnstufe> naechsteStufe(
            List<Mahnstufe> stufen, Mahnverlauf verlauf, LocalDate faelligAm, LocalDate stichtag) {
        if (verlauf.vorschlagOffen()) {
            return Optional.empty();
        }
        if (verlauf.gesperrtBis() != null && !stichtag.isAfter(verlauf.gesperrtBis())) {
            return Optional.empty();
        }
        var naechste = stufen.stream()
                .filter(stufe -> stufe.getStufe() == verlauf.letzteStufe() + 1)
                .findFirst();
        if (naechste.isEmpty()) {
            return Optional.empty();
        }
        var stufe = naechste.get();
        if (ChronoUnit.DAYS.between(faelligAm, stichtag) < stufe.getTageNachFaelligkeit()) {
            return Optional.empty();
        }
        if (verlauf.letztesMahndatum() != null
                && ChronoUnit.DAYS.between(verlauf.letztesMahndatum(), stichtag) < stufe.getMindestabstandTage()) {
            return Optional.empty();
        }
        return naechste;
    }
}
