package ch.kmu.debitoren.abgleich.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Die Zuordnungsstrategien und die Klärungsgründe, ohne Datenbank. */
class StrategienTest {

    private static final String REFERENZ = "000000000000000020260000018";

    private final Posten posten = new Posten();
    private final ReferenzExaktStrategie referenzExakt = new ReferenzExaktStrategie(posten);
    private final BetragUndKundeStrategie betragUndKunde = new BetragUndKundeStrategie(posten);

    @Test
    void referenzOrdnetAutomatischHoechstensDenOffenenBetragZu() {
        var rechnung = posten.mitReferenz(REFERENZ, "Keller Haustechnik AG", Postenzustand.ZAHLBAR, "250.00");

        var genau = referenzExakt.vorschlagen(fall("250.00", REFERENZ, null)).orElseThrow();
        assertThat(genau.automatisch()).isTrue();
        assertThat(genau.betrag()).isEqualTo(Geld.chf("250.00"));
        assertThat(genau.regel()).isEqualTo(ReferenzExaktStrategie.REGEL);
        assertThat(genau.posten()).isEqualTo(rechnung);

        assertThat(referenzExakt
                        .vorschlagen(fall("300.00", REFERENZ, null))
                        .orElseThrow()
                        .betrag())
                .as("Überzahlung: nur der offene Betrag")
                .isEqualTo(Geld.chf("250.00"));
        assertThat(referenzExakt
                        .vorschlagen(fall("100.00", REFERENZ, null))
                        .orElseThrow()
                        .betrag())
                .as("Teilzahlung")
                .isEqualTo(Geld.chf("100.00"));
    }

    @Test
    void referenzOhneTrefferErgibtKeinenVorschlagUndKeineAusnahme() {
        posten.mitReferenz(REFERENZ, "Keller Haustechnik AG", Postenzustand.STORNIERT, "0.00");

        assertThat(referenzExakt.vorschlagen(fall("250.00", REFERENZ, null))).isEmpty();
        assertThat(referenzExakt.vorschlagen(fall("250.00", "000000000000000020259999991", null)))
                .isEmpty();
        assertThat(referenzExakt.vorschlagen(fall("250.00", null, "Keller"))).isEmpty();
        assertThat(referenzExakt.vorschlagen(new Zahlungsfall(Ids.neu(), Geld.eur("250.00"), REFERENZ, null)))
                .isEmpty();
    }

    @Test
    void betragUndKundeSchlaegtNurVorUndNimmtDieAeltesteRechnung() {
        posten.zahlbar("RE-2026-0007", "Gasser Treuhand GmbH", LocalDate.of(2026, 9, 30), "95.50");
        var aeltere = posten.zahlbar("RE-2026-0003", "Gasser Treuhand GmbH", LocalDate.of(2026, 8, 31), "95.50");
        posten.zahlbar("RE-2026-0002", "Brunner Marco", LocalDate.of(2026, 7, 31), "95.50");

        var vorschlag = betragUndKunde
                .vorschlagen(fall("95.50", null, "GASSER TREUHAND"))
                .orElseThrow();

        assertThat(vorschlag.automatisch()).isFalse();
        assertThat(vorschlag.posten()).isEqualTo(aeltere);
        assertThat(vorschlag.begruendung()).isEqualTo("Betrag und Auftraggeber passen zu Rechnung RE-2026-0003");
        assertThat(betragUndKunde.vorschlagen(fall("95.50", null, "Lüthi Bau AG")))
                .isEmpty();
        assertThat(betragUndKunde.vorschlagen(fall("95.55", null, "Gasser Treuhand")))
                .isEmpty();
        assertThat(betragUndKunde.vorschlagen(fall("95.50", null, null))).isEmpty();
    }

    @Test
    void klaerungsgrundBenenntWasFehlt() {
        var ohne = fall("10.00", null, null);
        assertThat(Klaerungsgruende.bestimmen(ohne, Optional.empty())).isEqualTo(Klaerungsgrund.OHNE_REFERENZ);

        var mit = fall("10.00", REFERENZ, null);
        assertThat(Klaerungsgruende.bestimmen(mit, Optional.empty())).isEqualTo(Klaerungsgrund.REFERENZ_UNBEKANNT);
        assertThat(Klaerungsgruende.bestimmen(mit, Optional.of(posten(Postenzustand.STORNIERT, Geld.chf("0")))))
                .isEqualTo(Klaerungsgrund.RECHNUNG_STORNIERT);
        assertThat(Klaerungsgruende.bestimmen(mit, Optional.of(posten(Postenzustand.BEZAHLT, Geld.chf("0")))))
                .isEqualTo(Klaerungsgrund.RECHNUNG_BEREITS_BEZAHLT);
        assertThat(Klaerungsgruende.bestimmen(mit, Optional.of(posten(Postenzustand.ZAHLBAR, Geld.eur("10")))))
                .isEqualTo(Klaerungsgrund.WAEHRUNG_ABWEICHEND);
        assertThat(Klaerungsgruende.bestimmen(mit, Optional.of(posten(Postenzustand.ZAHLBAR, Geld.chf("10")))))
                .isEqualTo(Klaerungsgrund.REFERENZ_UNBEKANNT);
    }

    private static Zahlungsfall fall(String betrag, String referenz, String auftraggeber) {
        return new Zahlungsfall(Ids.neu(), Geld.chf(betrag), referenz, auftraggeber);
    }

    private static OffenerPosten posten(Postenzustand zustand, Geld offen) {
        return new OffenerPosten(Ids.neu(), "RE-2026-0001", "Keller", zustand, LocalDate.of(2026, 10, 1), offen, offen);
    }

    /** Offene Posten im Speicher, sortiert wie die echte Abfrage: älteste Fälligkeit zuerst. */
    private static final class Posten implements OffenePostenAbfrage {

        private final List<OffenerPosten> alle = new ArrayList<>();
        private final java.util.Map<String, OffenerPosten> nachReferenz = new java.util.HashMap<>();

        OffenerPosten mitReferenz(String referenz, String kunde, Postenzustand zustand, String offen) {
            var posten = new OffenerPosten(
                    Ids.neu(),
                    "RE-2026-0001",
                    kunde,
                    zustand,
                    LocalDate.of(2026, 10, 1),
                    Geld.chf("250.00"),
                    Geld.chf(offen));
            alle.add(posten);
            nachReferenz.put(referenz, posten);
            return posten;
        }

        OffenerPosten zahlbar(String nummer, String kunde, LocalDate faellig, String offen) {
            var posten = new OffenerPosten(
                    Ids.neu(), nummer, kunde, Postenzustand.ZAHLBAR, faellig, Geld.chf(offen), Geld.chf(offen));
            alle.add(posten);
            return posten;
        }

        @Override
        public Optional<OffenerPosten> finde(UUID rechnungId) {
            return alle.stream().filter(p -> p.rechnungId().equals(rechnungId)).findFirst();
        }

        @Override
        public Optional<OffenerPosten> nachReferenz(String referenz) {
            return Optional.ofNullable(nachReferenz.get(referenz));
        }

        @Override
        public List<OffenerPosten> zahlbareMitOffenemBetrag(Geld betrag) {
            return alle.stream()
                    .filter(OffenerPosten::istZahlbar)
                    .filter(p -> p.offen().equals(betrag))
                    .sorted(java.util.Comparator.comparing(OffenerPosten::faelligAm))
                    .toList();
        }
    }
}
