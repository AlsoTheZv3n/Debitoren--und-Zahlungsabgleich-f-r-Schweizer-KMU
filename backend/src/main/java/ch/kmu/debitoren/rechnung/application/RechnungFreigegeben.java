package ch.kmu.debitoren.rechnung.application;

import java.util.UUID;

/** Eine Rechnung hat Nummer und Referenz erhalten und ist offen; danach entsteht ihr PDF. */
public record RechnungFreigegeben(UUID mandantId, UUID rechnungId, String rechnungsnummer) {}
