package ch.kmu.debitoren.testdaten.api;

import ch.kmu.debitoren.shared.mandant.MandantFilter;
import ch.kmu.debitoren.testdaten.CamtTestdateien;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Nur im Profil {@code dev}: Testdateien für Demos, etwa über die Swagger-Oberfläche. */
@RestController
@Profile("dev")
@RequestMapping("/api/dev")
@Tag(name = "Entwicklung")
class CamtTestdateiController {

    private final CamtTestdateien testdateien;

    CamtTestdateiController(CamtTestdateien testdateien) {
        this.testdateien = testdateien;
    }

    @PostMapping(value = "/camt-testdatei", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            operationId = "camtTestdateiErzeugen",
            summary = "camt.054-Datei aus den offenen Rechnungen, mit jedem Fall des Abgleichs")
    @Parameter(
            name = MandantFilter.HEADER,
            in = ParameterIn.HEADER,
            description = "Mandant, nötig bei Benutzern mit mehreren Mandanten",
            schema = @Schema(type = "string", format = "uuid"))
    ResponseEntity<byte[]> erzeugen() {
        var datei = testdateien.erzeugen();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(datei.dateiname())
                                .build()
                                .toString())
                .body(datei.inhalt());
    }
}
