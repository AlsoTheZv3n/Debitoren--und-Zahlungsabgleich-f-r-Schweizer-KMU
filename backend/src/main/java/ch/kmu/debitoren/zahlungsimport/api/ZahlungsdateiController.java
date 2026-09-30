package ch.kmu.debitoren.zahlungsimport.api;

import ch.kmu.debitoren.shared.web.Seite;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungsImport;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungsdateienAbfragen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/zahlungsdateien")
@Tag(name = "Zahlungsdateien")
class ZahlungsdateiController {

    private final ZahlungsImport zahlungsImport;
    private final ZahlungsdateienAbfragen abfragen;

    ZahlungsdateiController(ZahlungsImport zahlungsImport, ZahlungsdateienAbfragen abfragen) {
        this.zahlungsImport = zahlungsImport;
        this.abfragen = abfragen;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "zahlungsdateiHochladen",
            summary = "camt.054-Datei importieren; eine bekannte Datei wird mit 409 abgelehnt")
    ZahlungsdateiDto hochladen(@RequestPart("datei") MultipartFile datei) throws IOException {
        return ZahlungsdateiDto.von(zahlungsImport.importieren(datei.getOriginalFilename(), datei.getBytes()));
    }

    @GetMapping
    @Operation(operationId = "zahlungsdateienAuflisten", summary = "Importprotokoll, neueste zuerst")
    Seite<ZahlungsdateiDto> auflisten(
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int groesse) {
        return Seite.von(abfragen.protokoll(PageRequest.of(seite, groesse)), ZahlungsdateiDto::von);
    }

    @GetMapping(value = "/{id}/original", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(operationId = "zahlungsdateiOriginal", summary = "Die importierte Datei, unverändert")
    @ApiResponse(
            responseCode = "200",
            description = "Die Datei, wie sie hochgeladen wurde",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_XML_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
    ResponseEntity<byte[]> original(@PathVariable UUID id) {
        var original = abfragen.original(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(original.dateiname())
                                .build()
                                .toString())
                .body(original.inhalt());
    }
}
