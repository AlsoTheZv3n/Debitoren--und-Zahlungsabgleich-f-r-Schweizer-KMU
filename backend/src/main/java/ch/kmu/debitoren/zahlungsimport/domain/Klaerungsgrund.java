package ch.kmu.debitoren.zahlungsimport.domain;

import org.springframework.modulith.NamedInterface;

/** Warum eine Zahlung in der Klärungsliste steht. */
@NamedInterface("modell")
public enum Klaerungsgrund {
    OHNE_REFERENZ("ohne Referenz"),
    REFERENZ_UNBEKANNT("Referenz unbekannt"),
    UEBERZAHLUNG("Überzahlung"),
    WAEHRUNG_ABWEICHEND("Währung passt nicht zur Rechnung"),
    RECHNUNG_STORNIERT("Rechnung storniert"),
    RECHNUNG_BEREITS_BEZAHLT("Rechnung bereits bezahlt"),
    RESTBETRAG("Restbetrag nach Zuordnung"),
    ZUORDNUNG_AUFGEHOBEN("Zuordnung aufgehoben");

    private final String text;

    Klaerungsgrund(String text) {
        this.text = text;
    }

    /** Kurzer Text für Protokolle; die Oberfläche übersetzt den Code selbst. */
    public String text() {
        return text;
    }
}
