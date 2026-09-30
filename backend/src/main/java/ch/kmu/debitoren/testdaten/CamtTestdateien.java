package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import ch.kmu.debitoren.testdaten.Camt054Schreiber.Testgutschrift;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Erzeugt im Profil {@code dev} eine camt.054-Datei aus den aktuell offenen Rechnungen, mit einem Beispiel
 * für jeden Fall des Abgleichs: exakte Zahlungen, eine Teil- und eine Überzahlung, eine Zahlung ohne Referenz
 * mit passendem Betrag und Namen und eine mit unbekannter Referenz. Ideal für Demos.
 */
@Service
@Profile("dev")
public class CamtTestdateien {

    private static final DateTimeFormatter STEMPEL =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.of("Europe/Zurich"));

    private final OffenePostenAbfragen offenePosten;
    private final MandantenAbfragen mandanten;
    private final Clock uhr;

    CamtTestdateien(OffenePostenAbfragen offenePosten, MandantenAbfragen mandanten, Clock uhr) {
        this.offenePosten = offenePosten;
        this.mandanten = mandanten;
        this.uhr = uhr;
    }

    public record Testdatei(String dateiname, byte[] inhalt) {}

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    public Testdatei erzeugen() {
        var mandant = mandanten.aktueller();
        var jetzt = Instant.now(uhr);
        var stempel = STEMPEL.format(jetzt);
        var heute = LocalDate.now(uhr);
        var posten = offenePosten.zahlbare(PageRequest.of(0, 8)).getContent();

        var gutschriften = new ArrayList<Testgutschrift>();
        for (int i = 0; i < posten.size(); i++) {
            var rechnung = posten.get(i);
            var bankReferenz = "DEV-%s-%d".formatted(stempel, i + 1);
            gutschriften.add(
                    switch (i) {
                        case 3 ->
                            mitReferenz(bankReferenz, heute, teilbetrag(rechnung.offen()), rechnung, "Teilzahlung");
                        case 4 ->
                            mitReferenz(
                                    bankReferenz,
                                    heute,
                                    rechnung.offen()
                                            .plus(Geld.von(
                                                    "25.00", rechnung.offen().waehrung())),
                                    rechnung,
                                    null);
                        case 5 ->
                            new Testgutschrift(
                                    bankReferenz,
                                    heute,
                                    rechnung.offen(),
                                    null,
                                    rechnung.kundeName().toUpperCase(),
                                    "Rechnung " + rechnung.rechnungsnummer());
                        default -> mitReferenz(bankReferenz, heute, rechnung.offen(), rechnung, null);
                    });
        }
        var unbekannt = mandant.referenzart() == Referenzart.QR
                ? Testreferenzen.qrReferenz("9999" + stempel.replace("-", "").substring(2))
                : Testreferenzen.scorReferenz("9999" + stempel.replace("-", "").substring(2));
        gutschriften.add(new Testgutschrift(
                "DEV-%s-UNBEKANNT".formatted(stempel), heute, Geld.chf("123.45"), unbekannt, "M. Muster", null));

        var inhalt = Camt054Schreiber.schreiben(
                "DEV-" + stempel, jetzt, mandant.zahlungskonto().wert(), List.copyOf(gutschriften));
        return new Testdatei("camt054-testdatei-%s.xml".formatted(stempel), inhalt);
    }

    private static Testgutschrift mitReferenz(
            String bankReferenz, LocalDate valuta, Geld betrag, Posten rechnung, String mitteilung) {
        return new Testgutschrift(bankReferenz, valuta, betrag, rechnung.referenz(), rechnung.kundeName(), mitteilung);
    }

    /** Etwa die Hälfte, auf 5 Rappen gerundet, mindestens 5 Franken. */
    static Geld teilbetrag(Geld offen) {
        var haelfte = offen.betrag().divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);
        var gerundet = new Geld(haelfte, offen.waehrung()).aufFuenfRappenGerundet();
        var minimum = Geld.von("5.00", offen.waehrung());
        return gerundet.istKleinerAls(minimum) || !gerundet.istKleinerAls(offen) ? offen : gerundet;
    }
}
