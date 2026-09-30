package ch.kmu.debitoren.shared.fehler;

/** Grobe Einordnung eines Fehlers; erst die Web-Schicht übersetzt sie in einen HTTP-Status. */
public enum Fehlerart {
    UNGUELTIG,
    NICHT_ANGEMELDET,
    VERBOTEN,
    NICHT_GEFUNDEN,
    KONFLIKT,
    INTERN
}
