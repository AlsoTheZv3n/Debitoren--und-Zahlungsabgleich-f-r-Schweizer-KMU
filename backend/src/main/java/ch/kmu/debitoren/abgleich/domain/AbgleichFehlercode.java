package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;

public enum AbgleichFehlercode implements Fehlercode {
    ZAHLUNG_BEREITS_ZUGEORDNET(Fehlerart.KONFLIKT, "Zahlung bereits zugeordnet"),
    ZAHLUNG_NOCH_IM_ABGLEICH(Fehlerart.KONFLIKT, "Zahlung noch im Abgleich"),
    ZUORDNUNG_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Zuordnung nicht möglich"),
    BETRAG_UNGUELTIG(Fehlerart.UNGUELTIG, "Betrag ungültig"),
    ZUORDNUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Zuordnung nicht gefunden"),
    ZAHLUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Zahlung nicht gefunden"),
    RECHNUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Rechnung nicht gefunden");

    private final Fehlerart art;
    private final String titel;

    AbgleichFehlercode(Fehlerart art, String titel) {
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
