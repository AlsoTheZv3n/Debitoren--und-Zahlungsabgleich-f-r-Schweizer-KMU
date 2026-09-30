package ch.kmu.debitoren.mahnwesen.api;

import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.MahnstufeDto;
import ch.kmu.debitoren.mahnwesen.api.MahnwesenDtos.MahnstufenAnfrageDto;
import ch.kmu.debitoren.mahnwesen.application.Mahnstufen;
import ch.kmu.debitoren.mahnwesen.application.Mahnstufen.Festlegung;
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
@RequestMapping("/api/mahnstufen")
@Tag(name = "Mahnstufen")
class MahnstufeController {

    private final Mahnstufen mahnstufen;

    MahnstufeController(Mahnstufen mahnstufen) {
        this.mahnstufen = mahnstufen;
    }

    @GetMapping
    @Operation(operationId = "mahnstufenAuflisten", summary = "Mahnstufen des Mandanten, erste Stufe zuerst")
    List<MahnstufeDto> auflisten() {
        return mahnstufen.auflisten().stream().map(MahnstufeDto::von).toList();
    }

    @PutMapping
    @Operation(
            operationId = "mahnstufenFestlegen",
            summary = "Alle Mahnstufen auf einmal festlegen; die Reihenfolge ergibt die Stufe")
    List<MahnstufeDto> festlegen(@Valid @RequestBody MahnstufenAnfrageDto anfrage) {
        return mahnstufen
                .festlegen(anfrage.stufen().stream()
                        .map(stufe -> new Festlegung(stufe.version(), stufe.alsDaten()))
                        .toList())
                .stream()
                .map(MahnstufeDto::von)
                .toList();
    }
}
