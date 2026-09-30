package ch.kmu.debitoren.reporting.application;

import ch.kmu.debitoren.reporting.domain.Altersstruktur;
import ch.kmu.debitoren.reporting.domain.Kennzahlenquelle;
import ch.kmu.debitoren.reporting.domain.Monatseingang;
import ch.kmu.debitoren.reporting.domain.Uebersicht;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kennzahlen, Altersstruktur und Zahlungseingänge für das Dashboard; alles gilt für heute in Schweizer Zeit. */
@Service
@Transactional(readOnly = true)
public class Auswertungen {

    private static final BigDecimal NULLBETRAG = BigDecimal.ZERO.setScale(2);

    private final Kennzahlenquelle quelle;
    private final Clock uhr;

    Auswertungen(Kennzahlenquelle quelle, Clock uhr) {
        this.quelle = quelle;
        this.uhr = uhr;
    }

    @PreAuthorize("hasRole('LESER')")
    public Uebersicht uebersicht() {
        return quelle.uebersicht(MandantKontext.erforderlich(), LocalDate.now(uhr));
    }

    @PreAuthorize("hasRole('LESER')")
    public Altersstruktur altersstruktur() {
        return quelle.altersstruktur(MandantKontext.erforderlich(), LocalDate.now(uhr));
    }

    /**
     * Eingänge der letzten Monate bis heute, lückenlos: Monate ohne Zahlung stehen mit null da, damit ein Diagramm
     * die Zeitachse richtig zeigt. Franken sind immer dabei, andere Währungen nur, wenn Zahlungen eingingen.
     */
    @PreAuthorize("hasRole('LESER')")
    public List<Monatseingang> zahlungseingaenge(int monate) {
        var bis = YearMonth.now(uhr);
        var von = bis.minusMonths(monate - 1L);
        var gefunden = quelle.zahlungseingaenge(MandantKontext.erforderlich(), von.atDay(1));
        var waehrungen = new TreeSet<Currency>(Comparator.comparing(Currency::getCurrencyCode));
        waehrungen.add(Geld.CHF);
        gefunden.forEach(eingang -> waehrungen.add(eingang.waehrung()));

        var jeSchluessel = new LinkedHashMap<String, Monatseingang>();
        gefunden.forEach(eingang -> jeSchluessel.put(schluessel(eingang.monat(), eingang.waehrung()), eingang));
        var ergebnis = new ArrayList<Monatseingang>();
        for (var monat = von; !monat.isAfter(bis); monat = monat.plusMonths(1)) {
            for (var waehrung : waehrungen) {
                var eingang = jeSchluessel.get(schluessel(monat, waehrung));
                ergebnis.add(eingang != null ? eingang : new Monatseingang(monat, waehrung, NULLBETRAG, NULLBETRAG, 0));
            }
        }
        return ergebnis;
    }

    private static String schluessel(YearMonth monat, Currency waehrung) {
        return monat + "/" + waehrung.getCurrencyCode();
    }
}
