package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.domain.Referenzart;
import java.util.Set;
import java.util.UUID;

/**
 * Was andere Module über den Mandanten wissen müssen, etwa für Rechnung und QR-Zahlteil.
 *
 * @param zahlungskonto Konto im QR-Zahlteil: die QR-IBAN, falls vorhanden, sonst die IBAN
 * @param konten alle Konten des Mandanten, also IBAN und QR-IBAN; Bankdateien müssen zu einem davon gehören
 */
public record MandantInfo(
        UUID id,
        String name,
        Adresse adresse,
        Iban zahlungskonto,
        Set<Iban> konten,
        Referenzart referenzart,
        String mwstNummer,
        String email,
        boolean rundungFuenfRappen) {}
