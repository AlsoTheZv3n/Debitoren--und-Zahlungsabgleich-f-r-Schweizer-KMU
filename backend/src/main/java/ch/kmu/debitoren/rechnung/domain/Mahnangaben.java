package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.time.LocalDate;
import org.springframework.modulith.NamedInterface;

/**
 * Was in einem Mahnschreiben steht. Das Mahnwesen hält die Beträge bei der Freigabe fest; das Schreiben zeigt
 * genau diesen Stand, auch wenn danach noch eine Zahlung eingeht.
 *
 * @param titel Bezeichnung der Stufe, etwa «1. Mahnung»
 * @param text Brieftext mit eingesetzten Platzhaltern; eine Leerzeile trennt Absätze
 * @param rechnungsbetrag Total der Rechnung
 * @param bezahlt bisherige Zahlungseingänge
 * @param mahngebuehren alle Gebühren, die der Kunde schuldet, auch die dieser Mahnung
 * @param offen was bis zur Frist zu zahlen ist; steht als Betrag im QR-Zahlteil
 */
@NamedInterface("modell")
public record Mahnangaben(
        String titel,
        LocalDate mahndatum,
        LocalDate fristBis,
        String text,
        Geld rechnungsbetrag,
        Geld bezahlt,
        Geld mahngebuehren,
        Geld offen) {}
