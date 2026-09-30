package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;

public enum RechnungFehlercode implements Fehlercode {
    RECHNUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Rechnung nicht gefunden"),
    NUR_ENTWURF_AENDERBAR(Fehlerart.KONFLIKT, "Nur Entwürfe sind änderbar"),
    FREIGABE_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Freigabe nicht möglich"),
    STORNO_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Storno nicht möglich"),
    MWST_SATZ_UNBEKANNT(Fehlerart.UNGUELTIG, "MWST-Satz unbekannt"),
    WAEHRUNG_NICHT_ERLAUBT(Fehlerart.UNGUELTIG, "Währung nicht erlaubt"),
    ZAHLTEIL_UNGUELTIG(Fehlerart.UNGUELTIG, "QR-Zahlteil ungültig"),
    PDF_NUR_NACH_FREIGABE(Fehlerart.KONFLIKT, "PDF erst nach der Freigabe"),
    ZAHLUNG_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Zahlung nicht möglich"),
    MAHNUNG_NICHT_MOEGLICH(Fehlerart.KONFLIKT, "Mahnung nicht möglich");

    private final Fehlerart art;
    private final String titel;

    RechnungFehlercode(Fehlerart art, String titel) {
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
