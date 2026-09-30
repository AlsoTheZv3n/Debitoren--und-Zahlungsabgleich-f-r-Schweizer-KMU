package ch.kmu.debitoren.mahnwesen.domain;

import org.springframework.modulith.NamedInterface;

/**
 * Lebenslauf einer Mahnung. Der Mahnlauf erzeugt Vorschläge; erst nach der Freigabe belastet die Gebühr die
 * Rechnung und das Schreiben geht hinaus. Freigegeben bleibt eine Mahnung, solange der Versand aussteht: bei
 * einem Fehler des Mailservers oder wenn sie per Post geht.
 */
@NamedInterface("modell")
public enum Mahnungsstatus {
    VORSCHLAG,
    FREIGEGEBEN,
    VERSENDET,
    VERWORFEN
}
