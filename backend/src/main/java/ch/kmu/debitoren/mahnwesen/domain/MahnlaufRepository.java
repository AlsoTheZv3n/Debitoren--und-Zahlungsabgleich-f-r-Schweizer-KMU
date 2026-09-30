package ch.kmu.debitoren.mahnwesen.domain;

import java.util.List;

/** Port für das Protokoll der Mahnläufe. */
public interface MahnlaufRepository {

    Mahnlauf speichern(Mahnlauf lauf);

    /** Die jüngsten Läufe zuerst. */
    List<Mahnlauf> letzte(int anzahl);
}
