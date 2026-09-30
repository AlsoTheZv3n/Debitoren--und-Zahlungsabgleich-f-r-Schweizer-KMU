package ch.kmu.debitoren.reporting.api;

import ch.kmu.debitoren.reporting.domain.Altersstruktur;
import ch.kmu.debitoren.reporting.domain.Altersstruktur.Band;
import ch.kmu.debitoren.reporting.domain.AuditEintrag;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Aktion;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import ch.kmu.debitoren.reporting.domain.Monatseingang;
import ch.kmu.debitoren.reporting.domain.Uebersicht;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Datenformate der API für Dashboard und Audit-Log. */
final class ReportingDtos {

    private ReportingDtos() {}

    record SummeDto(
            @NotNull @Schema(description = "ISO-Code, etwa CHF")
            String waehrung,

            @NotNull BigDecimal betrag,
            @NotNull long anzahl) {

        static SummeDto von(Uebersicht.Summe summe) {
            return new SummeDto(summe.waehrung().getCurrencyCode(), summe.betrag(), summe.anzahl());
        }
    }

    record ImportDto(
            @NotNull UUID id,
            @NotNull String dateiname,
            @NotNull Instant importiertAm,
            @NotNull String importiertVon,
            @NotNull int neu,
            @NotNull long inKlaerung) {}

    record MahnlaufInfoDto(
            @NotNull Instant ausgefuehrtAm,
            @NotNull String ausgeloestVon,
            @NotNull int vorschlaege) {}

    record UebersichtDto(
            @NotNull LocalDate stichtag,
            @NotNull List<SummeDto> offen,
            @NotNull List<SummeDto> ueberfaellig,

            @NotNull @Schema(description = "Rest der Zahlungen in der Klärungsliste")
            List<SummeDto> inKlaerung,

            @NotNull long mahnvorschlaege,
            @NotNull long entwuerfe,

            @NotNull @Schema(description = "Zahlungen der letzten 30 Tage nach Valuta")
            long zahlungen30Tage,

            @NotNull @Schema(description = "Davon ohne Zutun über die Referenz zugeordnet")
            long automatisch30Tage,

            @NotNull List<ImportDto> letzteImporte,
            MahnlaufInfoDto letzterMahnlauf) {

        static UebersichtDto von(Uebersicht uebersicht) {
            var lauf = uebersicht.letzterMahnlauf();
            return new UebersichtDto(
                    uebersicht.stichtag(),
                    uebersicht.offen().stream().map(SummeDto::von).toList(),
                    uebersicht.ueberfaellig().stream().map(SummeDto::von).toList(),
                    uebersicht.inKlaerung().stream().map(SummeDto::von).toList(),
                    uebersicht.mahnvorschlaege(),
                    uebersicht.entwuerfe(),
                    uebersicht.automatisierung().zahlungen(),
                    uebersicht.automatisierung().automatisch(),
                    uebersicht.letzteImporte().stream()
                            .map(importiert -> new ImportDto(
                                    importiert.id(),
                                    importiert.dateiname(),
                                    importiert.importiertAm(),
                                    importiert.importiertVon(),
                                    importiert.neu(),
                                    importiert.inKlaerung()))
                            .toList(),
                    lauf == null
                            ? null
                            : new MahnlaufInfoDto(lauf.ausgefuehrtAm(), lauf.ausgeloestVon(), lauf.vorschlaege()));
        }
    }

    record AltersbandDto(
            @NotNull Band band,
            @NotNull BigDecimal betrag,
            @NotNull long anzahl) {}

    record WaehrungsstrukturDto(
            @NotNull String waehrung,
            @NotNull List<AltersbandDto> baender,
            @NotNull BigDecimal total) {}

    record AltersstrukturDto(
            @NotNull LocalDate stichtag,

            @NotNull @Schema(description = "Je Währung alle fünf Bänder; Franken immer zuerst")
            List<WaehrungsstrukturDto> waehrungen) {

        static AltersstrukturDto von(Altersstruktur struktur) {
            return new AltersstrukturDto(
                    struktur.stichtag(),
                    struktur.waehrungen().stream()
                            .map(waehrung -> new WaehrungsstrukturDto(
                                    waehrung.waehrung().getCurrencyCode(),
                                    waehrung.baender().stream()
                                            .map(band -> new AltersbandDto(band.band(), band.betrag(), band.anzahl()))
                                            .toList(),
                                    waehrung.total()))
                            .toList());
        }
    }

    record MonatseingangDto(
            @NotNull @Schema(description = "Monat als yyyy-MM", example = "2026-09")
            String monat,

            @NotNull String waehrung,
            @NotNull BigDecimal betrag,

            @NotNull @Schema(description = "Davon automatisch über die Referenz zugeordnet")
            BigDecimal automatisch,

            @NotNull long anzahl) {

        static MonatseingangDto von(Monatseingang eingang) {
            return new MonatseingangDto(
                    eingang.monat().toString(),
                    eingang.waehrung().getCurrencyCode(),
                    eingang.betrag(),
                    eingang.automatisch(),
                    eingang.anzahl());
        }
    }

    record AenderungDto(@NotNull String feld, String vorher, String nachher) {}

    record AuditEintragDto(
            @NotNull long revision,
            @NotNull Instant zeitpunkt,
            @NotNull String benutzer,
            @NotNull Objektart objektart,
            @NotNull UUID objektId,
            @NotNull Aktion aktion,
            @NotNull String bezeichnung,
            UUID rechnungId,
            @NotNull List<AenderungDto> aenderungen) {

        static AuditEintragDto von(AuditEintrag eintrag) {
            return new AuditEintragDto(
                    eintrag.revision(),
                    eintrag.zeitpunkt(),
                    eintrag.benutzer(),
                    eintrag.objektart(),
                    eintrag.objektId(),
                    eintrag.aktion(),
                    eintrag.bezeichnung(),
                    eintrag.rechnungId(),
                    eintrag.aenderungen().stream()
                            .map(aenderung ->
                                    new AenderungDto(aenderung.feld(), aenderung.vorher(), aenderung.nachher()))
                            .toList());
        }
    }
}
