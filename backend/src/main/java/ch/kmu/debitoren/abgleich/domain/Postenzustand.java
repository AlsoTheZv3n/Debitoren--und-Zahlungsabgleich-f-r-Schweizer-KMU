package ch.kmu.debitoren.abgleich.domain;

/** Ob eine Rechnung noch Zahlungen annimmt. */
public enum Postenzustand {
    /** Offen oder teilbezahlt. */
    ZAHLBAR,
    BEZAHLT,
    STORNIERT
}
