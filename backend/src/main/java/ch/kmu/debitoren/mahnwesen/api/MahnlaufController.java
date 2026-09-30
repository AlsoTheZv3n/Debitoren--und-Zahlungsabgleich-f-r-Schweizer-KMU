package ch.kmu.debitoren.mahnwesen.api;

import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.MahnlaufDto;
import ch.kmu.debitoren.mahnwesen.application.Mahnlaeufe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mahnlaeufe")
@Tag(name = "Mahnungen")
class MahnlaufController {

    private final Mahnlaeufe laeufe;

    MahnlaufController(Mahnlaeufe laeufe) {
        this.laeufe = laeufe;
    }

    @GetMapping
    @Operation(operationId = "mahnlaeufeAuflisten", summary = "Die letzten Mahnläufe, neueste zuerst")
    List<MahnlaufDto> auflisten(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int anzahl) {
        return laeufe.letzte(anzahl).stream().map(MahnlaufDto::von).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "mahnlaufAusfuehren", summary = "Mahnlauf mit Stichtag heute von Hand auslösen")
    MahnlaufDto ausfuehren() {
        return MahnlaufDto.von(laeufe.ausfuehren(laeufe.heute()));
    }
}
