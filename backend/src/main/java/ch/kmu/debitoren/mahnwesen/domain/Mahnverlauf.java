package ch.kmu.debitoren.mahnwesen.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Was eine Rechnung bisher an Mahnungen erlebt hat, soweit es für die nächste Stufe zählt.
 *
 * @param letzteStufe höchste freigegebene Stufe; 0 ohne Mahnung
 * @param letztesMahndatum Datum dieser Mahnung; null ohne Mahnung
 * @param vorschlagOffen ein Vorschlag wartet noch auf Freigabe
 * @param gesperrtBis bis zu diesem Tag schlägt der Mahnlauf nichts vor, weil jemand einen Vorschlag verworfen hat
 */
public record Mahnverlauf(int letzteStufe, LocalDate letztesMahndatum, boolean vorschlagOffen, LocalDate gesperrtBis) {

    public static final Mahnverlauf OHNE_MAHNUNG = new Mahnverlauf(0, null, false, null);

    public static Mahnverlauf aus(Collection<Mahnung> mahnungen) {
        var letzte = mahnungen.stream()
                .filter(mahnung -> mahnung.getStatus() == Mahnungsstatus.FREIGEGEBEN
                        || mahnung.getStatus() == Mahnungsstatus.VERSENDET)
                .max(Comparator.comparingInt(Mahnung::getStufe));
        var vorschlag = mahnungen.stream().anyMatch(mahnung -> mahnung.getStatus() == Mahnungsstatus.VORSCHLAG);
        var sperre = mahnungen.stream()
                .filter(mahnung -> mahnung.getStatus() == Mahnungsstatus.VERWORFEN)
                .map(Mahnung::getGesperrtBis)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new Mahnverlauf(
                letzte.map(Mahnung::getStufe).orElse(0),
                letzte.map(Mahnung::getMahndatum).orElse(null),
                vorschlag,
                sperre);
    }
}
