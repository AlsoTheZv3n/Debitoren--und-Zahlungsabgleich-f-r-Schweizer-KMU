package ch.kmu.debitoren.rechnung.api;

import ch.kmu.debitoren.rechnung.api.RechnungDtos.MwstSatzDto;
import ch.kmu.debitoren.rechnung.application.MwstSaetze;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mwst-saetze")
@Tag(name = "MWST")
class MwstSatzController {

    private final MwstSaetze saetze;
    private final RechnungAbbildung abbildung;

    MwstSatzController(MwstSaetze saetze, RechnungAbbildung abbildung) {
        this.saetze = saetze;
        this.abbildung = abbildung;
    }

    @GetMapping
    @Operation(
            operationId = "mwstSaetzeAuflisten",
            summary = "MWST-Sätze des Mandanten; mit Stichtag nur die an diesem Tag gültigen")
    List<MwstSatzDto> auflisten(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stichtag) {
        return abbildung.zuSatzDtos(saetze.auflisten(stichtag));
    }
}
