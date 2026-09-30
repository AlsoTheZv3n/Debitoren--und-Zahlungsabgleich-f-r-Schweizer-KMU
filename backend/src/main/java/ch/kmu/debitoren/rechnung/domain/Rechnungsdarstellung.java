package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Iban;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

/**
 * Port für die Darstellung einer Rechnung: PDF mit QR-Zahlteil und der Zahlteil allein als SVG. Die
 * Umsetzung prüft den Zahlteil nach den Swiss Payment Standards.
 */
public interface Rechnungsdarstellung {

    byte[] pdf(Rechnungsdokument dokument);

    String zahlteilSvg(Zahlteil zahlteil);

    /** Verstösse gegen die Swiss Payment Standards; leer, wenn der Zahlteil gültig ist. */
    List<String> zahlteilPruefen(Zahlteil zahlteil);

    /** Absender der Rechnung, zugleich Zahlungsempfänger im Zahlteil. */
    record Rechnungssteller(String name, Adresse adresse, Iban konto, String mwstNummer, String email) {}

    /**
     * Inhalt des QR-Zahlteils. Ohne Betrag entsteht ein Zahlteil zum Ausfüllen; Rechnungsangaben landen als
     * Swico-S1-Information im QR-Code, damit die Buchhaltung des Zahlers sie übernehmen kann.
     */
    record Zahlteil(
            Rechnungssteller empfaenger,
            Currency waehrung,
            Geld betrag,
            Empfaenger zahler,
            Zahlungsreferenz referenz,
            String mitteilung,
            Rechnungsangaben rechnungsangaben) {}

    /** Angaben für Swico S1. */
    record Rechnungsangaben(
            String rechnungsnummer,
            LocalDate rechnungsdatum,
            String kundennummer,
            String mwstNummer,
            List<Summen.MwstAnteil> mwst,
            int zahlungsfristTage) {}

    record Rechnungsdokument(Rechnungssteller steller, Rechnung rechnung, String kundennummer, Zahlteil zahlteil) {}
}
