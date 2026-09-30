package ch.kmu.debitoren.abgleich.api;

import ch.kmu.debitoren.abgleich.api.AbgleichDtos.VorschlagDto;
import ch.kmu.debitoren.abgleich.api.AbgleichDtos.ZahlungDto;
import ch.kmu.debitoren.abgleich.api.AbgleichDtos.ZuordnungAnfrageDto;
import ch.kmu.debitoren.abgleich.api.AbgleichDtos.ZuordnungDto;
import ch.kmu.debitoren.abgleich.application.Klaerungsliste;
import ch.kmu.debitoren.abgleich.application.Zahlungsabgleich;
import ch.kmu.debitoren.shared.web.Seite;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zahlungen")
@Tag(name = "Zahlungen")
class ZahlungController {

    private final Klaerungsliste klaerungsliste;
    private final Zahlungsabgleich abgleich;

    ZahlungController(Klaerungsliste klaerungsliste, Zahlungsabgleich abgleich) {
        this.klaerungsliste = klaerungsliste;
        this.abgleich = abgleich;
    }

    @GetMapping
    @Operation(
            operationId = "zahlungenSuchen",
            summary = "Zahlungen mit Rest, neueste Valuta zuerst; mit status=KLAERUNG die Klärungsliste")
    Seite<ZahlungDto> suchen(
            @RequestParam(required = false) Zahlungsstatus status,
            @RequestParam(required = false) UUID zahlungsdateiId,
            @RequestParam(required = false) String suche,
            @RequestParam(defaultValue = "0") @Min(0) int seite,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int groesse) {
        return Seite.von(
                klaerungsliste.zahlungen(status, zahlungsdateiId, suche, PageRequest.of(seite, groesse)),
                ZahlungDto::von);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "zahlungLesen", summary = "Eine Zahlung mit Rest und Klärungsgrund")
    ZahlungDto lesen(@PathVariable UUID id) {
        return ZahlungDto.von(klaerungsliste.zahlung(id));
    }

    @GetMapping("/{id}/vorschlaege")
    @Operation(operationId = "zahlungVorschlaege", summary = "Zuordnungsvorschläge aller Strategien für den Rest")
    List<VorschlagDto> vorschlaege(@PathVariable UUID id) {
        return abgleich.vorschlaege(id).stream().map(VorschlagDto::von).toList();
    }

    @GetMapping("/{id}/zuordnungen")
    @Operation(operationId = "zahlungZuordnungen", summary = "Zuordnungen dieser Zahlung")
    List<ZuordnungDto> zuordnungen(@PathVariable UUID id) {
        return klaerungsliste.zuordnungenDerZahlung(id).stream()
                .map(ZuordnungDto::von)
                .toList();
    }

    @PostMapping("/{id}/zuordnungen")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "zahlungZuordnen",
            summary = "Zahlung von Hand einer Rechnung zuordnen; eine veraltete Version ergibt 409")
    ZahlungDto zuordnen(@PathVariable UUID id, @Valid @RequestBody ZuordnungAnfrageDto anfrage) {
        abgleich.zuordnen(id, anfrage.version(), anfrage.rechnungId(), anfrage.betrag());
        return ZahlungDto.von(klaerungsliste.zahlung(id));
    }
}
