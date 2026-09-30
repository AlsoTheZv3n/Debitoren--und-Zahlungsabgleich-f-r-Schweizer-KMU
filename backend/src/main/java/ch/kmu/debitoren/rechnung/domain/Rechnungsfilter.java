package ch.kmu.debitoren.rechnung.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Filter der Rechnungsliste; {@code null} heisst jeweils «ohne Einschränkung». */
public record Rechnungsfilter(
        Rechnungsstatus status,
        UUID kundeId,
        boolean nurOffene,
        boolean nurUeberfaellige,
        LocalDate faelligBis,
        String suchtext) {

    public static Rechnungsfilter keiner() {
        return new Rechnungsfilter(null, null, false, false, null, null);
    }
}
