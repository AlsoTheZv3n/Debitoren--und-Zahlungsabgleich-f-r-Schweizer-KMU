package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.MwstSatz;
import ch.kmu.debitoren.rechnung.domain.MwstSatzRepository;
import ch.kmu.debitoren.rechnung.domain.SchweizerMwstSaetze;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
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
