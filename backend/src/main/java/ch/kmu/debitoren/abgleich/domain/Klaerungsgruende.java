package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import java.util.Optional;

/** Warum der Abgleich eine Zahlung nicht automatisch zuordnen konnte. */
public final class Klaerungsgruende {

    private Klaerungsgruende() {}

    /**
     * @param nachReferenz die Rechnung mit der Referenz der Zahlung, falls es eine gibt
     */
    public static Klaerungsgrund bestimmen(Zahlungsfall fall, Optional<OffenerPosten> nachReferenz) {
        if (!fall.hatReferenz()) {
            return Klaerungsgrund.OHNE_REFERENZ;
        }
        if (nachReferenz.isEmpty()) {
            return Klaerungsgrund.REFERENZ_UNBEKANNT;
        }
        var posten = nachReferenz.get();
        return switch (posten.zustand()) {
            case STORNIERT -> Klaerungsgrund.RECHNUNG_STORNIERT;
            case BEZAHLT -> Klaerungsgrund.RECHNUNG_BEREITS_BEZAHLT;
            case ZAHLBAR ->
                posten.offen().waehrung().equals(fall.rest().waehrung())
                        ? Klaerungsgrund.REFERENZ_UNBEKANNT
                        : Klaerungsgrund.WAEHRUNG_ABWEICHEND;
        };
    }
}
