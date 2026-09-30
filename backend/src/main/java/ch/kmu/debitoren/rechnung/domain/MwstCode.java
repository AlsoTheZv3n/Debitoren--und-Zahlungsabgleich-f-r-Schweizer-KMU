package ch.kmu.debitoren.rechnung.domain;

import org.springframework.modulith.NamedInterface;

/** Art des MWST-Satzes; der Prozentsatz hängt vom Datum ab und steht in {@link MwstSatz}. */
@NamedInterface("modell")
public enum MwstCode {
    NORMAL("Normalsatz"),
    REDUZIERT("Reduzierter Satz"),
    BEHERBERGUNG("Sondersatz Beherbergung"),
    OHNE("Ohne MWST");

    private final String bezeichnung;

    MwstCode(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String bezeichnung() {
        return bezeichnung;
    }
}
