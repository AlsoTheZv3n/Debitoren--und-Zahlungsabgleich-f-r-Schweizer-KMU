package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.shared.web.Seite;
import ch.kmu.debitoren.stammdaten.application.KundenAbfragen;
import ch.kmu.debitoren.stammdaten.application.KundenVerwaltung;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kunden")
@Tag(name = "Kunden")
class KundeController {

    private final KundenAbfragen abfragen;
    private final KundenVerwaltung verwaltung;
    private final KundenMapper mapper;

    KundeController(KundenAbfragen abfragen, KundenVerwaltung verwaltung, KundenMapper mapper) {
        this.abfragen = abfragen;
        this.verwaltung = verwaltung;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(operationId = "kundenSuchen", summary = "Kunden suchen, nach Name sortiert")
    Seite<KundeDto> suchen(
            @RequestParam(required = false) String suche,
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int groesse) {
        return Seite.von(abfragen.suchen(suche, seite, groesse), mapper::zuDto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "kundeAnlegen", summary = "Kunden anlegen")
    KundeDto anlegen(@Valid @RequestBody KundeEingabeDto eingabe) {
        return mapper.zuDto(verwaltung.anlegen(mapper.zuDaten(eingabe)));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "kundeLesen", summary = "Kunden lesen")
    KundeDto lesen(@PathVariable UUID id) {
        return mapper.zuDto(abfragen.lesen(id));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "kundeAendern", summary = "Kunden ändern")
    KundeDto aendern(@PathVariable UUID id, @Valid @RequestBody KundeAenderungDto aenderung) {
        return mapper.zuDto(verwaltung.aendern(id, aenderung.version(), mapper.zuDaten(aenderung)));
    }
}
