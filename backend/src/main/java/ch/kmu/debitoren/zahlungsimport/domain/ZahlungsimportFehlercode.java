package ch.kmu.debitoren.zahlungsimport.domain;

import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;

public enum ZahlungsimportFehlercode implements Fehlercode {
    DATEI_BEREITS_IMPORTIERT(Fehlerart.KONFLIKT, "Datei bereits importiert"),
    DATEI_UNGUELTIG(Fehlerart.UNGUELTIG, "Datei ungültig"),
    KONTO_UNBEKANNT(Fehlerart.UNGUELTIG, "Konto gehört nicht zum Mandanten"),
    ZAHLUNGSDATEI_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Zahlungsdatei nicht gefunden"),
    ZAHLUNG_NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Zahlung nicht gefunden");

    private final Fehlerart art;
    private final String titel;

    ZahlungsimportFehlercode(Fehlerart art, String titel) {
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
