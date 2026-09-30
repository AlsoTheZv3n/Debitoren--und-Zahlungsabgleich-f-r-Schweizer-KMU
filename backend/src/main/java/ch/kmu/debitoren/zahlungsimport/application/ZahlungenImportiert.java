package ch.kmu.debitoren.zahlungsimport.application;

import java.util.List;
import java.util.UUID;

/** Eine Bankdatei ist importiert; die neuen Zahlungen warten auf den Abgleich. */
public record ZahlungenImportiert(UUID mandantId, UUID zahlungsdateiId, List<UUID> zahlungIds) {

    public ZahlungenImportiert {
        zahlungIds = List.copyOf(zahlungIds);
    }
}
