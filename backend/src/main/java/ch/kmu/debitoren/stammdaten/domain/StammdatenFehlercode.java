package ch.kmu.debitoren.stammdaten.domain;

import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;

public enum StammdatenFehlercode implements Fehlercode {
    KUNDE_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Kunde nicht gefunden"),
    KUNDENNUMMER_VERGEBEN(Fehlerart.KONFLIKT, "Kundennummer bereits vergeben"),
    MANDANT_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Mandant nicht gefunden");

    private final Fehlerart art;
    private final String titel;

    StammdatenFehlercode(Fehlerart art, String titel) {
        this.art = art;
        this.titel = titel;
    }

    @Override
    public Fehlerart art() {
        return art;
    }

    @Override
    public String titel() {
        return titel;
    }
}
