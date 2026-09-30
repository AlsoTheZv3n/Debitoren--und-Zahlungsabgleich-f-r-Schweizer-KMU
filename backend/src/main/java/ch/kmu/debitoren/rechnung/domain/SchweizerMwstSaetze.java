package ch.kmu.debitoren.rechnung.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Die gesetzlichen Sätze der Eidgenössischen Steuerverwaltung seit 2018. Jeder neue Mandant erhält sie
 * als Grundkonfiguration; Admins pflegen sie danach selbst.
 */
public final class SchweizerMwstSaetze {

    private static final LocalDate AB_2018 = LocalDate.of(2018, 1, 1);
    private static final LocalDate BIS_2023 = LocalDate.of(2023, 12, 31);
    private static final LocalDate AB_2024 = LocalDate.of(2024, 1, 1);

    private SchweizerMwstSaetze() {}

    public static List<MwstSatz> neueSaetze() {
        return List.of(
                satz(MwstCode.NORMAL, "7.70", AB_2018, BIS_2023),
                satz(MwstCode.NORMAL, "8.10", AB_2024, null),
                satz(MwstCode.REDUZIERT, "2.50", AB_2018, BIS_2023),
                satz(MwstCode.REDUZIERT, "2.60", AB_2024, null),
                satz(MwstCode.BEHERBERGUNG, "3.70", AB_2018, BIS_2023),
                satz(MwstCode.BEHERBERGUNG, "3.80", AB_2024, null),
                satz(MwstCode.OHNE, "0.00", AB_2018, null));
    }

    private static MwstSatz satz(MwstCode code, String prozent, LocalDate ab, LocalDate bis) {
        return new MwstSatz(code, code.bezeichnung(), new BigDecimal(prozent), ab, bis);
    }
}
