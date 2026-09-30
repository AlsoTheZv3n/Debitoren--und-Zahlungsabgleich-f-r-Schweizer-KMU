package ch.kmu.debitoren.shared.fehler;

/** Fehlercodes, die nicht zu einem einzelnen Fachmodul gehören. */
public enum AllgemeinerFehlercode implements Fehlercode {
    VALIDIERUNG_FEHLGESCHLAGEN(Fehlerart.UNGUELTIG, "Eingaben ungültig"),
    UNGUELTIGER_WERT(Fehlerart.UNGUELTIG, "Ungültiger Wert"),
    ANFRAGE_UNGUELTIG(Fehlerart.UNGUELTIG, "Anfrage ungültig"),
    NICHT_ANGEMELDET(Fehlerart.NICHT_ANGEMELDET, "Nicht angemeldet"),
    ZUGRIFF_VERWEIGERT(Fehlerart.VERBOTEN, "Zugriff verweigert"),
    MANDANT_FEHLT(Fehlerart.UNGUELTIG, "Kein Mandant gewählt"),
    MANDANT_UNGUELTIG(Fehlerart.UNGUELTIG, "Mandant ungültig"),
    MANDANT_NICHT_ERLAUBT(Fehlerart.VERBOTEN, "Mandant nicht erlaubt"),
    NICHT_GEFUNDEN(Fehlerart.NICHT_GEFUNDEN, "Nicht gefunden"),
    BEREITS_VORHANDEN(Fehlerart.KONFLIKT, "Bereits vorhanden"),
    GLEICHZEITIG_GEAENDERT(Fehlerart.KONFLIKT, "Gleichzeitig geändert"),
    INTERNER_FEHLER(Fehlerart.INTERN, "Interner Fehler");

    private final Fehlerart art;
    private final String titel;

    AllgemeinerFehlercode(Fehlerart art, String titel) {
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
