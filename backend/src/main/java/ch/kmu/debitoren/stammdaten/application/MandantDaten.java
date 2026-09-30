package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;

/** Angaben eines Mandanten, wie ein Admin sie in den Einstellungen ändert. */
public record MandantDaten(
        String name,
        Adresse adresse,
        Iban iban,
        Iban qrIban,
        String mwstNummer,
        String email,
        boolean rundungFuenfRappen) {}
