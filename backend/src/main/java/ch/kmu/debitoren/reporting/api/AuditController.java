package ch.kmu.debitoren.reporting.api;

import ch.kmu.debitoren.reporting.api.ReportingDtos.AuditEintragDto;
import ch.kmu.debitoren.reporting.application.AuditLog;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import ch.kmu.debitoren.shared.web.Seite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit")
class AuditController {

    private final AuditLog auditLog;

    AuditController(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping
    @Operation(operationId = "auditLogLesen", summary = "Audit-Log des Mandanten, neueste Änderung zuerst (Admin)")
    Seite<AuditEintragDto> eintraege(
            @RequestParam(required = false) Objektart objektart,
            @RequestParam(required = false) String benutzer,
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int groesse) {
        return Seite.von(auditLog.eintraege(objektart, benutzer, PageRequest.of(seite, groesse)), AuditEintragDto::von);
    }

    @GetMapping("/rechnung/{rechnungId}")
    @Operation(
            operationId = "verlaufDerRechnung",
            summary = "Verlauf einer Rechnung samt Zahlungseingängen und Mahnungen, neueste Änderung zuerst")
    List<AuditEintragDto> verlaufDerRechnung(@PathVariable UUID rechnungId) {
        return auditLog.verlaufDerRechnung(rechnungId).stream()
                .map(AuditEintragDto::von)
                .toList();
    }
}
