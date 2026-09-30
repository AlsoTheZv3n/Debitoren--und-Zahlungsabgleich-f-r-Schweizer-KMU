package ch.kmu.debitoren.zahlungsimport.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Eine Zeile des Importprotokolls: Angaben zur Datei und der aktuelle Stand ihrer Zahlungen.
 *
 * @param unbearbeitet neue Zahlungen, die der Abgleich noch nicht bearbeitet hat
 */
public record ZahlungsdateiZeile(
        UUID id,
        String dateiname,
        String nachrichtId,
        Instant erstelltVonBankAm,
        Instant importiertAm,
        String importiertVon,
        int anzahlBuchungen,
        int anzahlNeu,
        int anzahlUebersprungen,
        int anzahlIgnoriert,
        long unbearbeitet,
        long zugeordnet,
        long inKlaerung) {}
