package ch.kmu.debitoren.rechnung.api;

import ch.kmu.debitoren.rechnung.api.RechnungDtos.EntwurfAenderungDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.EntwurfDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.RechnungDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.RechnungZeileDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.Rechnungssortierung;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.StornoDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.VersionDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.VorschauAnfrageDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.VorschauDto;
import ch.kmu.debitoren.rechnung.application.RechnungAbfragen;
import ch.kmu.debitoren.rechnung.application.RechnungVerwaltung;
import ch.kmu.debitoren.rechnung.application.Rechnungsdokumente;
import ch.kmu.debitoren.rechnung.domain.Rechnungsfilter;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.web.Seite;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/rechnungen")
@Tag(name = "Rechnungen")
class RechnungController {

    private static final String SVG = "image/svg+xml";

    private final RechnungAbfragen abfragen;
    private final RechnungVerwaltung verwaltung;
    private final Rechnungsdokumente dokumente;
    private final RechnungAbbildung abbildung;

    RechnungController(
            RechnungAbfragen abfragen,
            RechnungVerwaltung verwaltung,
            Rechnungsdokumente dokumente,
            RechnungAbbildung abbildung) {
        this.abfragen = abfragen;
        this.verwaltung = verwaltung;
        this.dokumente = dokumente;
        this.abbildung = abbildung;
    }

    @GetMapping
    @Operation(operationId = "rechnungenSuchen", summary = "Rechnungen mit Filtern, paginiert")
    Seite<RechnungZeileDto> suchen(
            @RequestParam(required = false) Rechnungsstatus status,
            @RequestParam(required = false) UUID kundeId,
            @RequestParam(defaultValue = "false") boolean offen,
            @RequestParam(defaultValue = "false") boolean ueberfaellig,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate faelligBis,
            @RequestParam(required = false) String suche,
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int groesse,
            @RequestParam(defaultValue = "DATUM_ABSTEIGEND") Rechnungssortierung sortierung) {
        var filter = new Rechnungsfilter(status, kundeId, offen, ueberfaellig, faelligBis, suche);
        var heute = abfragen.heute();
        return Seite.von(
                abfragen.suchen(filter, PageRequest.of(seite, groesse, sortierung(sortierung))),
                zeile -> abbildung.zuDto(zeile, heute));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "entwurfAnlegen", summary = "Rechnungsentwurf anlegen")
    RechnungDto anlegen(@Valid @RequestBody EntwurfDto entwurf) {
        return dto(verwaltung.entwurfAnlegen(abbildung.zuEntwurf(entwurf)));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "rechnungLesen", summary = "Rechnung mit Positionen und Summen")
    RechnungDto lesen(@PathVariable UUID id) {
        return dto(abfragen.lesen(id));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "entwurfAendern", summary = "Entwurf bearbeiten")
    RechnungDto aendern(@PathVariable UUID id, @Valid @RequestBody EntwurfAenderungDto aenderung) {
        return dto(verwaltung.entwurfAendern(id, aenderung.version(), abbildung.zuEntwurf(aenderung.entwurf())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "entwurfLoeschen", summary = "Entwurf löschen")
    void loeschen(@PathVariable UUID id, @RequestParam long version) {
        verwaltung.entwurfLoeschen(id, version);
    }

    @PostMapping("/{id}/freigabe")
    @Operation(operationId = "rechnungFreigeben", summary = "Entwurf freigeben: Nummer, Referenz und PDF")
    RechnungDto freigeben(@PathVariable UUID id, @Valid @RequestBody VersionDto version) {
        return dto(verwaltung.freigeben(id, version.version()));
    }

    @PostMapping("/{id}/storno")
    @Operation(operationId = "rechnungStornieren", summary = "Offene Rechnung stornieren")
    RechnungDto stornieren(@PathVariable UUID id, @Valid @RequestBody StornoDto storno) {
        return dto(verwaltung.stornieren(id, storno.version(), storno.grund()));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(operationId = "rechnungPdf", summary = "Rechnungs-PDF mit QR-Zahlteil")
    ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        var pdf = dokumente.pdf(id);
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

    @GetMapping(value = "/{id}/zahlteil.svg", produces = SVG)
    @Operation(operationId = "rechnungZahlteil", summary = "Vorschau des QR-Zahlteils")
    ResponseEntity<String> zahlteil(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(SVG)).body(dokumente.zahlteilSvg(id));
    }

    @PostMapping("/vorschau")
    @Operation(operationId = "rechnungVorschau", summary = "Summen und Zahlteil eines ungespeicherten Entwurfs")
    VorschauDto vorschau(@Valid @RequestBody VorschauAnfrageDto anfrage) {
        var vorschau = abfragen.vorschau(abbildung.zuEntwurf(anfrage));
        return new VorschauDto(
                abbildung.zuDto(vorschau.summen()), vorschau.positionsbetraege(), vorschau.zahlteilSvg());
    }

    private RechnungDto dto(ch.kmu.debitoren.rechnung.domain.Rechnung rechnung) {
        return abbildung.zuDto(rechnung, abfragen.heute(), abfragen.offenerBetrag(rechnung.getId()));
    }

    private static Sort sortierung(Rechnungssortierung sortierung) {
        return switch (sortierung) {
            case DATUM_ABSTEIGEND -> Sort.by(Sort.Order.desc("rechnungsdatum"), Sort.Order.desc("rechnungsnummer"));
            case DATUM_AUFSTEIGEND -> Sort.by(Sort.Order.asc("rechnungsdatum"), Sort.Order.asc("rechnungsnummer"));
            case FAELLIGKEIT_AUFSTEIGEND -> Sort.by(Sort.Order.asc("faelligAm"), Sort.Order.asc("rechnungsnummer"));
            case NUMMER_ABSTEIGEND -> Sort.by(Sort.Order.desc("rechnungsnummer"));
            case TOTAL_ABSTEIGEND -> Sort.by(Sort.Order.desc("total"), Sort.Order.desc("rechnungsdatum"));
        };
    }
}
