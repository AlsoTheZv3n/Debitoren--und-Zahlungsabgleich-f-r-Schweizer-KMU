package ch.kmu.debitoren.reporting.application;

import ch.kmu.debitoren.reporting.domain.AuditEintrag;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import ch.kmu.debitoren.reporting.domain.Auditquelle;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nachvollziehbarkeit: wer wann was geändert hat. Das ganze Log sieht nur die Administration; den Verlauf einer
 * Rechnung sieht jeder, der die Rechnung sehen darf.
 */
@Service
@Transactional(readOnly = true)
public class AuditLog {

    private final Auditquelle quelle;

    AuditLog(Auditquelle quelle) {
        this.quelle = quelle;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditEintrag> eintraege(Objektart objektart, String benutzer, Pageable seite) {
        var filter = benutzer == null || benutzer.isBlank() ? null : benutzer.strip();
        return quelle.eintraege(MandantKontext.erforderlich(), objektart, filter, seite);
    }

    @PreAuthorize("hasRole('LESER')")
    public List<AuditEintrag> verlaufDerRechnung(UUID rechnungId) {
        return quelle.verlaufDerRechnung(MandantKontext.erforderlich(), rechnungId);
    }
}
