package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Pruefe;
import java.math.BigDecimal;
import java.util.List;

/**
 * Inhalt einer Mahnstufe, wie ein Admin ihn festlegt. Die Nummer der Stufe ergibt sich aus der Reihenfolge.
 *
 * @param tageNachFaelligkeit frühestens so viele Tage nach der Fälligkeit wird diese Stufe vorgeschlagen
 * @param mindestabstandTage frühestens so viele Tage nach der vorherigen Stufe; bei der ersten ohne Bedeutung
 * @param fristTage Zahlungsfrist im Schreiben, gezählt ab dem Mahndatum
 * @param gebuehr Gebühr in der Währung der Rechnung; 0 für keine
 * @param textvorlage Brieftext mit Platzhaltern, siehe {@link Mahntext}
 */
public record Mahnstufendaten(
        String bezeichnung,
        int tageNachFaelligkeit,
        int mindestabstandTage,
        int fristTage,
        BigDecimal gebuehr,
        String textvorlage) {

    public static final int HOECHSTE_STUFENZAHL = 5;

    /** Eine bis fünf Stufen, jede später nach der Fälligkeit als die vorherige. */
    public static void folgePruefen(List<Mahnstufendaten> stufen) {
        Pruefe.vorhanden(stufen, "Mahnstufen");
        if (stufen.isEmpty() || stufen.size() > HOECHSTE_STUFENZAHL) {
            throw Pruefe.ungueltig("Es braucht eine bis %d Mahnstufen".formatted(HOECHSTE_STUFENZAHL));
        }
        for (int i = 1; i < stufen.size(); i++) {
            if (stufen.get(i).tageNachFaelligkeit() <= stufen.get(i - 1).tageNachFaelligkeit()) {
                throw Pruefe.ungueltig(
                        "Stufe %d muss später nach der Fälligkeit kommen als Stufe %d".formatted(i + 1, i));
            }
        }
    }
}
