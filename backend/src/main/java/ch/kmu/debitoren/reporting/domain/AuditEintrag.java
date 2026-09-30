package ch.kmu.debitoren.reporting.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Eine Änderung aus dem Audit-Log: wer wann was an welchem Objekt geändert hat.
 *
 * @param revision Nummer der Revision; alle Änderungen einer Transaktion teilen sie
 * @param bezeichnung lesbarer Name des Objekts, etwa «RE-2026-0412 · Keller Haustechnik»
 * @param rechnungId Rechnung, zu der das Objekt gehört; null bei Kunden, Mandant und Konfiguration
 * @param aenderungen geänderte Felder; beim Anlegen die wichtigsten Angaben, beim Löschen der letzte Stand
 */
public record AuditEintrag(
        long revision,
        Instant zeitpunkt,
        String benutzer,
        Objektart objektart,
        UUID objektId,
        Aktion aktion,
        String bezeichnung,
        UUID rechnungId,
        List<Aenderung> aenderungen) {

    public enum Objektart {
        RECHNUNG,
        ZUORDNUNG,
        MAHNUNG,
        MAHNSTUFE,
        KUNDE,
        MANDANT,
        MWST_SATZ
    }

    public enum Aktion {
        ANGELEGT,
        GEAENDERT,
        GELOESCHT;

        /** Aus dem Revisionstyp von Envers: 0 angelegt, 1 geändert, 2 gelöscht. */
        public static Aktion ausRevisionstyp(int revtyp) {
            return switch (revtyp) {
                case 0 -> ANGELEGT;
                case 1 -> GEAENDERT;
                case 2 -> GELOESCHT;
                default -> throw new IllegalArgumentException("Unbekannter Revisionstyp " + revtyp);
            };
        }
    }

    /** Ein Feld mit Wert vorher und nachher, beide schon lesbar formatiert; null steht für leer. */
    public record Aenderung(String feld, String vorher, String nachher) {}
}
