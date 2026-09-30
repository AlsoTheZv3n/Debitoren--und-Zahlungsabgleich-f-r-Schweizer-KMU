package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.stammdaten.application.MandantVerwaltung;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mandanten")
@Tag(name = "Mandanten")
class MandantController {

    private final MandantenAbfragen abfragen;
    private final MandantVerwaltung verwaltung;
    private final MandantMapper mapper;

    MandantController(MandantenAbfragen abfragen, MandantVerwaltung verwaltung, MandantMapper mapper) {
        this.abfragen = abfragen;
        this.verwaltung = verwaltung;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            operationId = "mandantenAuflisten",
            summary = "Mandanten, für die der angemeldete Benutzer arbeiten darf")
    List<MandantDto> auflisten() {
        return mapper.zuDtos(abfragen.erlaubteMandanten());
    }

    @GetMapping("/aktuell")
    @Operation(operationId = "mandantLesen", summary = "Alle Angaben des gewählten Mandanten")
    MandantDetailDto lesen() {
        return MandantDetailDto.von(abfragen.aktuellerMandant());
    }

    @PutMapping("/aktuell")
    @Operation(
            operationId = "mandantAendern",
            summary = "Gewählten Mandanten ändern; freigegebene Rechnungen behalten Konto und Referenz")
    MandantDetailDto aendern(@Valid @RequestBody MandantAenderungDto aenderung) {
        return MandantDetailDto.von(verwaltung.aendern(aenderung.version(), aenderung.alsDaten()));
    }
}
