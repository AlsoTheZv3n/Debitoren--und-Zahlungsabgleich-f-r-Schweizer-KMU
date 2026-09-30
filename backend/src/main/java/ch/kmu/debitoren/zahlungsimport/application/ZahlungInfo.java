package ch.kmu.debitoren.zahlungsimport.application;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlung;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.time.LocalDate;
import java.util.UUID;

/** Was der Abgleich über eine Zahlung wissen muss. */
public record ZahlungInfo(
        UUID id,
        long version,
        UUID zahlungsdateiId,
        String bankReferenz,
        LocalDate valuta,
        Geld betrag,
        String referenz,
        String auftraggeber,
        String auftraggeberIban,
        String mitteilung,
        Zahlungsstatus status,
        Klaerungsgrund klaerungsgrund) {

    static ZahlungInfo von(Zahlung zahlung) {
        return new ZahlungInfo(
                zahlung.getId(),
                zahlung.getVersion() == null ? 0 : zahlung.getVersion(),
                zahlung.getZahlungsdateiId(),
                zahlung.getBankReferenz(),
                zahlung.getValuta(),
                zahlung.getBetrag(),
                zahlung.getReferenz(),
                zahlung.getAuftraggeber(),
                zahlung.getAuftraggeberIban(),
                zahlung.getMitteilung(),
                zahlung.getStatus(),
                zahlung.getKlaerungsgrund());
    }
}
