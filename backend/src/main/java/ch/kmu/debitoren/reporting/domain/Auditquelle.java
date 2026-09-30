package ch.kmu.debitoren.reporting.domain;

import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für das Audit-Log in den {@code _aud}-Tabellen; die Umsetzung filtert selbst nach Mandant. */
public interface Auditquelle {

    /** Alle Änderungen des Mandanten, neueste zuerst; Objektart und Benutzer schränken ein, wenn gesetzt. */
    Page<AuditEintrag> eintraege(UUID mandant, Objektart objektart, String benutzer, Pageable seite);

    /** Verlauf einer Rechnung samt ihrer Zuordnungen und Mahnungen, neueste Änderung zuerst. */
    List<AuditEintrag> verlaufDerRechnung(UUID mandant, UUID rechnungId);
}
