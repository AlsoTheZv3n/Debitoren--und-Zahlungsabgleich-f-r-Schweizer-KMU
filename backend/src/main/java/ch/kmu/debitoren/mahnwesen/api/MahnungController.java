package ch.kmu.debitoren.mahnwesen.api;

import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.FreigabeAnfrageDto;
import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.FreigabeErgebnisDto;
import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.MahnungDto;
import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.VersionDto;
import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.VerwerfenDto;
import ch.kmu.debitoren.mahnwesen.application.MahnungAbfragen;
import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe;
import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe.Auswahl;
import ch.kmu.debitoren.mahnwesen.application.Mahnungsversand;
import ch.kmu.debitoren.mahnwesen.domain.MahnungZeile;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.shared.web.Seite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mahnungen")
@Tag(name = "Mahnungen")
class MahnungController {

    private final MahnungAbfragen abfragen;
    private final MahnungFreigabe freigabe;
    private final Mahnungsversand versand;

    MahnungController(MahnungAbfragen abfragen, MahnungFreigabe freigabe, Mahnungsversand versand) {
        this.abfragen = abfragen;
        this.freigabe = freigabe;
        this.versand = versand;
    }

    @GetMapping
    @Operation(
            operationId = "mahnungenSuchen",
            summary = "Mahnungen; mit status=VORSCHLAG die Vorschläge des Mahnlaufs, älteste Fälligkeit zuerst")
    Seite<MahnungDto> suchen(
            @RequestParam(required = false) Mahnungsstatus status,
            @RequestParam(required = false) UUID rechnungId,
            @RequestParam(required = false) String suche,
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(500) int groesse) {
        var heute = abfragen.heute();
        return Seite.von(
                abfragen.suchen(status, rechnungId, suche, PageRequest.of(seite, groesse)),
                zeile -> MahnungDto.von(zeile, heute));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "mahnungLesen", summary = "Eine Mahnung")
    MahnungDto lesen(@PathVariable UUID id) {
        return dto(abfragen.lesen(id));
    }

    @GetMapping("/rechnung/{rechnungId}")
    @Operation(operationId = "mahnungenDerRechnung", summary = "Alle Mahnungen einer Rechnung, erste Stufe zuerst")
    List<MahnungDto> derRechnung(@PathVariable UUID rechnungId) {
        var heute = abfragen.heute();
        return abfragen.derRechnung(rechnungId).stream()
                .map(zeile -> MahnungDto.von(zeile, heute))
                .toList();
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            operationId = "mahnungPdf",
            summary = "Mahnschreiben mit QR-Zahlteil; bei Vorschlägen eine Vorschau mit dem heutigen Stand")
    ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        var pdf = versand.pdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(pdf.dateiname())
                                .build()
                                .toString())
                .body(pdf.inhalt());
    }

    @PostMapping("/freigabe")
    @Operation(
            operationId = "mahnungenFreigeben",
            summary = "Vorschläge gesammelt freigeben; jede Mahnung einzeln, Konflikte stehen im Ergebnis")
    FreigabeErgebnisDto freigeben(@Valid @RequestBody FreigabeAnfrageDto anfrage) {
        return FreigabeErgebnisDto.von(freigabe.freigeben(anfrage.mahnungen().stream()
                .map(gewaehlt -> new Auswahl(gewaehlt.id(), gewaehlt.version()))
                .toList()));
    }

    @PostMapping("/{id}/verwerfen")
    @Operation(operationId = "mahnungVerwerfen", summary = "Vorschlag verwerfen, auf Wunsch mit Sperre")
    MahnungDto verwerfen(@PathVariable UUID id, @Valid @RequestBody VerwerfenDto verwerfen) {
        freigabe.verwerfen(id, verwerfen.version(), verwerfen.grund(), verwerfen.gesperrtBis());
        return dto(abfragen.lesen(id));
    }

    @PostMapping("/{id}/versand")
    @Operation(operationId = "mahnungErneutSenden", summary = "Nach einem Versandfehler erneut per E-Mail senden")
    MahnungDto erneutSenden(@PathVariable UUID id, @Valid @RequestBody VersionDto version) {
        versand.erneutSenden(id, version.version());
        return dto(abfragen.lesen(id));
    }

    @PostMapping("/{id}/postversand")
    @Operation(operationId = "mahnungPerPostVersendet", summary = "Bestätigen, dass das Schreiben per Post ging")
    MahnungDto perPostVersendet(@PathVariable UUID id, @Valid @RequestBody VersionDto version) {
        versand.perPostVersendet(id, version.version());
        return dto(abfragen.lesen(id));
    }

    private MahnungDto dto(MahnungZeile zeile) {
        return MahnungDto.von(zeile, abfragen.heute());
    }
}
