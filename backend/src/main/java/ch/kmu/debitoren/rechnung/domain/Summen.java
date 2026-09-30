package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.util.List;

/** Beträge einer Rechnung: netto, MWST je Satz, Rundung auf 5 Rappen und Total. */
public record Summen(Geld netto, List<MwstAnteil> mwst, Geld mwstTotal, Geld rundung, Geld total) {

    public Summen {
        mwst = List.copyOf(mwst);
    }

    /** MWST eines Satzes: Basis ist die Summe der Nettobeträge mit diesem Satz. */
    public record MwstAnteil(MwstCode code, BigDecimal prozent, Geld basis, Geld betrag) {}
}
