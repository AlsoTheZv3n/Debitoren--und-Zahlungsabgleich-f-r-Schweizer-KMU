package ch.kmu.debitoren.zahlungsimport.application;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlung;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungRepository;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsimportFehlercode;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stand einer Zahlung im Abgleich. Das Modul Abgleich ruft diese Methoden in seiner eigenen Transaktion
 * auf, zusammen mit den Zuordnungen und dem Zahlungsstand der Rechnung.
 */
@Service
public class Zahlungsstand {

    private final ZahlungRepository zahlungen;

    Zahlungsstand(ZahlungRepository zahlungen) {
        this.zahlungen = zahlungen;
    }

    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public ZahlungInfo lesen(UUID id) {
        MandantKontext.erforderlich();
        return ZahlungInfo.von(zahlungen.finde(id).orElseThrow(() -> nichtGefunden(id)));
    }

    /**
     * Sperrt die Zahlung für eine Änderung: Die Version muss der erwarteten entsprechen und steigt beim
     * Commit. Wer eine veraltete Zahlung zuordnen will, erhält einen Konflikt.
     *
     * @param erwarteteVersion Version, die der Benutzer gesehen hat; {@code null} im automatischen Abgleich
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional(propagation = Propagation.MANDATORY)
    public ZahlungInfo sperren(UUID id, Long erwarteteVersion) {
        var zahlung = laden(id);
        if (erwarteteVersion != null && !Objects.equals(zahlung.getVersion(), erwarteteVersion)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Die Zahlung wurde inzwischen geändert. Bitte neu laden.");
        }
        return ZahlungInfo.von(zahlung);
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional(propagation = Propagation.MANDATORY)
    public void zugeordnet(UUID id) {
        laden(id).zugeordnet();
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional(propagation = Propagation.MANDATORY)
    public void inKlaerung(UUID id, Klaerungsgrund grund) {
        laden(id).inKlaerung(grund);
    }

    private Zahlung laden(UUID id) {
        MandantKontext.erforderlich();
        return zahlungen.zumAendern(id).orElseThrow(() -> nichtGefunden(id));
    }

    private static FachlicherFehler nichtGefunden(UUID id) {
        return new FachlicherFehler(
                ZahlungsimportFehlercode.ZAHLUNG_NICHT_GEFUNDEN, "Zahlung %s nicht gefunden".formatted(id));
    }
}
