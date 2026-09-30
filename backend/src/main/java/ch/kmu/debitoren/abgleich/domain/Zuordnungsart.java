package ch.kmu.debitoren.abgleich.domain;

/** Wer eine Zuordnung erstellt hat. */
public enum Zuordnungsart {
    /** Der Abgleich nach dem Import, nur über eine eindeutige Referenz. */
    AUTO,
    /** Ein Benutzer in der Klärungsliste. */
    MANUELL
}
