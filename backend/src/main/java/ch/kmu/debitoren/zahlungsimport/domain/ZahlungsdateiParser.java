package ch.kmu.debitoren.zahlungsimport.domain;

/**
 * Port für das Lesen von Bankdateien. Heute liest ein Adapter camt.054; ein Parser für camt.053 erfüllt
 * denselben Vertrag.
 */
public interface ZahlungsdateiParser {

    /**
     * Liest und prüft eine Datei vollständig, bevor etwas gespeichert wird.
     *
     * @throws ch.kmu.debitoren.shared.fehler.FachlicherFehler mit {@link ZahlungsimportFehlercode#DATEI_UNGUELTIG},
     *     wenn die Datei kein gültiges Dokument des erwarteten Schemas ist
     */
    Zahlungsdateiinhalt lesen(byte[] inhalt);
}
