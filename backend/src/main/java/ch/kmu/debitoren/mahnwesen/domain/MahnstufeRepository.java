package ch.kmu.debitoren.mahnwesen.domain;

import java.util.List;

/** Port für die Mahnstufen des Mandanten. */
public interface MahnstufeRepository {

    /** Alle Stufen, nach Nummer sortiert. */
    List<Mahnstufe> alle();

    boolean vorhanden();

    Mahnstufe speichern(Mahnstufe stufe);

    void loeschen(Mahnstufe stufe);
}
