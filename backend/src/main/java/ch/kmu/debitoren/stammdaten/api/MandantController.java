package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mandanten")
@Tag(name = "Mandanten")
class MandantController {

    private final MandantenAbfragen abfragen;
    private final MandantMapper mapper;

    MandantController(MandantenAbfragen abfragen, MandantMapper mapper) {
        this.abfragen = abfragen;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            operationId = "mandantenAuflisten",
            summary = "Mandanten, für die der angemeldete Benutzer arbeiten darf")
    List<MandantDto> auflisten() {
        return mapper.zuDtos(abfragen.erlaubteMandanten());
    }
}
