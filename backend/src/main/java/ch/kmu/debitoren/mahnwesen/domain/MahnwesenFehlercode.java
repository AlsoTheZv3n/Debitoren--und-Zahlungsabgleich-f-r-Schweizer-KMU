package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;

public enum MahnwesenFehlercode implements Fehlercode {
    MAHNUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Mahnung nicht gefunden"),
    NUR_VORSCHLAG_MOEGLICH(Fehlerart.KONFLIKT, "Nur bei Vorschlägen möglich"),
    VERSAND_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Versand nicht möglich"),
    KEINE_MAHNSTUFEN(Fehlerart.KONFLIKT, "Keine Mahnstufen");

    private final Fehlerart art;
    private final String titel;

    MahnwesenFehlercode(Fehlerart art, String titel) {
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
