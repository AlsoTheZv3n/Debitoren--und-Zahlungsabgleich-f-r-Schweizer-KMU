package ch.kmu.debitoren.shared.fehler;

/**
 * Fachlicher Fehlercode, der im Feld {@code code} des ProblemDetail erscheint. Jedes Modul definiert
 * seine eigenen Codes als Enum, etwa {@code ZAHLUNG_BEREITS_ZUGEORDNET}.
 */
public interface Fehlercode {

    /** Stabiler Name, den Clients auswerten dürfen. */
    String name();

    Fehlerart art();

    /** Kurze, für Menschen lesbare Überschrift. */
    String titel();
}
