package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.domain.Adresse;
import java.util.UUID;

/** Was andere Module über einen Kunden wissen müssen, etwa für den Empfänger einer Rechnung. */
public record KundeInfo(
        UUID id, String kundennummer, String name, Adresse adresse, String email, int zahlungsfristTage) {}
