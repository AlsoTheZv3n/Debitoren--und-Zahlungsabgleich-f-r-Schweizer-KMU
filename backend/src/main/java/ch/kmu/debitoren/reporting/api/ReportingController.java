package ch.kmu.debitoren.reporting.api;

import ch.kmu.debitoren.reporting.api.ReportingDtos.AltersstrukturDto;
import ch.kmu.debitoren.reporting.api.ReportingDtos.MonatseingangDto;
import ch.kmu.debitoren.reporting.api.ReportingDtos.UebersichtDto;
import ch.kmu.debitoren.reporting.application.Auswertungen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporting")
@Tag(name = "Reporting")
class ReportingController {

    private final Auswertungen auswertungen;

    ReportingController(Auswertungen auswertungen) {
        this.auswertungen = auswertungen;
    }

    @GetMapping("/uebersicht")
    @Operation(operationId = "uebersichtLesen", summary = "Kennzahlen für das Dashboard")
    UebersichtDto uebersicht() {
        return UebersichtDto.von(auswertungen.uebersicht());
    }

    @GetMapping("/altersstruktur")
    @Operation(
            operationId = "altersstrukturLesen",
            summary = "Offene Posten nach Tagen über Fälligkeit: nicht fällig, 1–30, 31–60, 61–90, über 90")
    AltersstrukturDto altersstruktur() {
        return AltersstrukturDto.von(auswertungen.altersstruktur());
    }

    @GetMapping("/zahlungseingaenge")
    @Operation(
            operationId = "zahlungseingaengeLesen",
            summary = "Zahlungseingänge je Monat und Währung bis heute, ohne Lücken")
    List<MonatseingangDto> zahlungseingaenge(@RequestParam(defaultValue = "12") @Min(1) @Max(36) int monate) {
        return auswertungen.zahlungseingaenge(monate).stream()
                .map(MonatseingangDto::von)
                .toList();
    }
}
