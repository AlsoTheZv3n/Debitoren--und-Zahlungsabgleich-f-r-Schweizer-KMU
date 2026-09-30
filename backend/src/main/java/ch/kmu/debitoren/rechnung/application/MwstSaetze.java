package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.MwstCode;
import ch.kmu.debitoren.rechnung.domain.MwstSatz;
import ch.kmu.debitoren.rechnung.domain.MwstSatzRepository;
import ch.kmu.debitoren.rechnung.domain.SchweizerMwstSaetze;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MwstSaetze {

    private final MwstSatzRepository saetze;

    MwstSaetze(MwstSatzRepository saetze) {
        this.saetze = saetze;
    }

    /** Alle Sätze oder, mit Stichtag, nur die an diesem Tag gültigen. */
    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public List<MwstSatz> auflisten(LocalDate stichtag) {
        MandantKontext.erforderlich();
        var alle = saetze.alle();
        return stichtag == null
                ? alle
                : alle.stream().filter(satz -> satz.giltAm(stichtag)).toList();
    }

    /**
     * Erfasst einen neuen Satz ab einem Stichtag, etwa nach einer Änderung durch den Bund. Der bisher geltende Satz
     * desselben Codes endet am Vortag; Rechnungen vor dem Stichtag rechnen weiter mit ihm.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MwstSatz neuerSatz(MwstCode code, BigDecimal satz, LocalDate gueltigAb) {
        MandantKontext.erforderlich();
        Pruefe.vorhanden(code, "MWST-Code");
        Pruefe.vorhanden(gueltigAb, "Gültig ab");
        var bisherige = saetze.alle().stream()
                .filter(bisher -> bisher.getCode() == code)
                .toList();
        var spaeter = bisherige.stream()
                .filter(bisher -> !bisher.getGueltigAb().isBefore(gueltigAb))
                .findFirst();
        if (spaeter.isPresent()) {
            throw Pruefe.ungueltig("Für %s gibt es bereits einen Satz ab %s; ein neuer Satz muss später beginnen"
                    .formatted(code.bezeichnung(), spaeter.get().getGueltigAb()));
        }
        bisherige.stream()
                .filter(bisher -> bisher.getGueltigBis() == null
                        || !bisher.getGueltigBis().isBefore(gueltigAb))
                .forEach(bisher -> bisher.beenden(gueltigAb.minusDays(1)));
        return saetze.speichern(new MwstSatz(code, code.bezeichnung(), satz, gueltigAb, null));
    }

    /** Richtet die gesetzlichen Sätze ein, sofern der Mandant noch keine hat. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void standardSaetzeAnlegen() {
        MandantKontext.erforderlich();
        if (!saetze.vorhanden()) {
            SchweizerMwstSaetze.neueSaetze().forEach(saetze::speichern);
        }
    }
}
