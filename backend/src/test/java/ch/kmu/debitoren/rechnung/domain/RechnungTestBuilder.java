package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/** Baut Rechnungen für Tests; jeder Test beschreibt nur, was für ihn zählt. */
public final class RechnungTestBuilder {

    public static final MwstSatz NORMAL_2024 =
            new MwstSatz(MwstCode.NORMAL, "Normalsatz", new BigDecimal("8.10"), LocalDate.of(2024, 1, 1), null);
    public static final MwstSatz REDUZIERT_2024 = new MwstSatz(
            MwstCode.REDUZIERT, "Reduzierter Satz", new BigDecimal("2.60"), LocalDate.of(2024, 1, 1), null);
    public static final MwstSatz NORMAL_2018 = new MwstSatz(
            MwstCode.NORMAL,
            "Normalsatz",
            new BigDecimal("7.70"),
            LocalDate.of(2018, 1, 1),
            LocalDate.of(2023, 12, 31));

    private UUID kundeId = UUID.fromString("019a0000-0000-7000-8000-00000000c001");
    private Empfaenger empfaenger =
            new Empfaenger("Keller Haustechnik AG", Adresse.schweiz("Industriestrasse", "7", "3052", "Zollikofen"));
    private LocalDate datum = LocalDate.of(2026, 9, 1);
    private int frist = 30;
    private Currency waehrung = Geld.CHF;
    private String bemerkung;
    private boolean rundung = true;
    private final List<Positionsdaten> positionen = new ArrayList<>();

    public static RechnungTestBuilder rechnung() {
        return new RechnungTestBuilder();
    }

    public RechnungTestBuilder datum(LocalDate datum) {
        this.datum = datum;
        return this;
    }

    public RechnungTestBuilder frist(int tage) {
        this.frist = tage;
        return this;
    }

    public RechnungTestBuilder waehrung(Currency waehrung) {
        this.waehrung = waehrung;
        return this;
    }

    public RechnungTestBuilder bemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
        return this;
    }

    public RechnungTestBuilder ohneRundung() {
        this.rundung = false;
        return this;
    }

    public RechnungTestBuilder empfaenger(Empfaenger empfaenger) {
        this.empfaenger = empfaenger;
        return this;
    }

    public RechnungTestBuilder position(String beschreibung, String menge, String preis, MwstSatz satz) {
        positionen.add(new Positionsdaten(beschreibung, new BigDecimal(menge), "Std", new BigDecimal(preis), satz));
        return this;
    }

    public RechnungTestBuilder standardposition() {
        return position("Arbeitsstunde", "2", "100.00", NORMAL_2024);
    }

    public Rechnungskopf kopf() {
        return new Rechnungskopf(datum, frist, waehrung, bemerkung);
    }

    public List<Positionsdaten> positionen() {
        return List.copyOf(positionen);
    }

    public Empfaenger empfaenger() {
        return empfaenger;
    }

    public Rechnung entwurf() {
        return Rechnung.entwurf(kundeId, empfaenger, kopf(), positionen(), rundung);
    }
}
