package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import java.util.UUID;

/** Angaben für einen neuen Mandanten; die ID muss mit dem Claim {@code mandant_id} in Keycloak übereinstimmen. */
public record NeuerMandant(
        UUID id, String name, Adresse adresse, Iban iban, Iban qrIban, String mwstNummer, String email) {}
