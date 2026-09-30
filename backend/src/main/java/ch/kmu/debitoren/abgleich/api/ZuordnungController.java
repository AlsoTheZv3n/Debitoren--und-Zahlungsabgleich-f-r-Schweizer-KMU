package ch.kmu.debitoren.abgleich.api;

import ch.kmu.debitoren.abgleich.api.AbgleichDtos.ZuordnungDto;
import ch.kmu.debitoren.abgleich.application.Klaerungsliste;
import ch.kmu.debitoren.abgleich.application.Zahlungsabgleich;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zuordnungen")
@Tag(name = "Zuordnungen")
class ZuordnungController {

    private final Klaerungsliste klaerungsliste;
    private final Zahlungsabgleich abgleich;

    ZuordnungController(Klaerungsliste klaerungsliste, Zahlungsabgleich abgleich) {
        this.klaerungsliste = klaerungsliste;
        this.abgleich = abgleich;
    }

    @GetMapping
    @Operation(operationId = "zuordnungenDerRechnung", summary = "Zahlungseingänge einer Rechnung")
    List<ZuordnungDto> derRechnung(@RequestParam UUID rechnungId) {
        return klaerungsliste.zuordnungenDerRechnung(rechnungId).stream()
                .map(ZuordnungDto::von)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            operationId = "zuordnungAufheben",
            summary = "Zuordnung aufheben; die Zahlung geht in die Klärung, die Rechnung wird wieder offen")
    void aufheben(@PathVariable UUID id) {
        abgleich.aufheben(id);
    }
}
