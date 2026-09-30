package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Eine Mahnung für Listen und Detailansicht, mit dem heute offenen Betrag der Rechnung aus der Sicht
 * {@code v_mahnungen}.
 *
 * @param offenerBetrag Betrag im Schreiben; erst ab der Freigabe gesetzt
 * @param aktuellOffen was heute auf der Rechnung offen ist, ohne die Gebühr eines Vorschlags
 */
@NamedInterface("modell")
public record MahnungZeile(
        UUID id,
        long version,
        UUID rechnungId,
        String rechnungsnummer,
        UUID kundeId,
        String kundeName,
        LocalDate faelligAm,
        int stufe,
        String bezeichnung,
        LocalDate mahndatum,
        LocalDate fristBis,
        Geld gebuehr,
        Mahnungsstatus status,
        Geld offenerBetrag,
        Geld aktuellOffen,
        Versandweg versandweg,
        String empfaengerEmail,
        Instant freigegebenAm,
        String freigegebenVon,
        Instant versendetAm,
        String versandfehler,
        Instant verworfenAm,
        String verworfenVon,
        String verworfenGrund,
        LocalDate gesperrtBis,
        Instant erstelltAm) {

    /** Was das Schreiben verlangt: bei Vorschlägen der heutige offene Betrag plus die Gebühr dieser Stufe. */
    public Geld zuZahlen() {
        return status == Mahnungsstatus.VORSCHLAG || offenerBetrag == null ? aktuellOffen.plus(gebuehr) : offenerBetrag;
    }
}
