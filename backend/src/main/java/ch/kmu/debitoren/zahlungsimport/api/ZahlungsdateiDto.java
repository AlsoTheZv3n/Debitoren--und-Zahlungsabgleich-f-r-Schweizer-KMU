package ch.kmu.debitoren.zahlungsimport.api;

import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Eine Zeile des Importprotokolls. */
record ZahlungsdateiDto(
        @NotNull UUID id,
        @NotNull String dateiname,
        @NotNull String nachrichtId,
        @NotNull Instant erstelltVonBankAm,
        @NotNull Instant importiertAm,
        @NotNull String importiertVon,

        @NotNull @Schema(description = "Gutschriften in der Datei")
        int anzahlBuchungen,

        @NotNull @Schema(description = "Als Zahlung gespeichert")
        int anzahlNeu,

        @NotNull @Schema(description = "Schon aus einer früheren Datei bekannt")
        int anzahlUebersprungen,

        @NotNull @Schema(description = "Belastungen, Stornos und Vormerkungen")
        int anzahlIgnoriert,

        @NotNull @Schema(description = "Noch nicht abgeglichen")
        long unbearbeitet,

        @NotNull long zugeordnet,
        @NotNull long inKlaerung) {

    static ZahlungsdateiDto von(ZahlungsdateiZeile zeile) {
        return new ZahlungsdateiDto(
                zeile.id(),
                zeile.dateiname(),
                zeile.nachrichtId(),
                zeile.erstelltVonBankAm(),
                zeile.importiertAm(),
                zeile.importiertVon(),
                zeile.anzahlBuchungen(),
                zeile.anzahlNeu(),
                zeile.anzahlUebersprungen(),
                zeile.anzahlIgnoriert(),
                zeile.unbearbeitet(),
                zeile.zugeordnet(),
                zeile.inKlaerung());
    }
}
