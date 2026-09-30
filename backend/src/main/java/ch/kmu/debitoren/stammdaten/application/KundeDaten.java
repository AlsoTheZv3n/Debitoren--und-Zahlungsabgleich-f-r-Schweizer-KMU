package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.domain.Adresse;

/** Eingaben für einen Kunden. Ohne Kundennummer vergibt die Anwendung die nächste freie. */
public record KundeDaten(String kundennummer, String name, Adresse adresse, String email, int zahlungsfristTage) {}
