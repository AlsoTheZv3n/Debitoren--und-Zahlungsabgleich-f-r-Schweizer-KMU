package ch.kmu.debitoren.mahnwesen.api;

import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe.Ablehnung;
import ch.kmu.debitoren.mahnwesen.application.MahnungFreigabe.Freigabeergebnis;
import ch.kmu.debitoren.mahnwesen.domain.Mahnlauf;
import ch.kmu.debitoren.mahnwesen.domain.Mahnstufe;
import ch.kmu.debitoren.mahnwesen.domain.Mahnstufendaten;
import ch.kmu.debitoren.mahnwesen.domain.MahnungZeile;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.mahnwesen.domain.Versandweg;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Datenformate der API für Mahnungen, Mahnläufe und Mahnstufen. */
final class MahnwesenDtos {

    private MahnwesenDtos() {}

    record MahnungDto(
            @NotNull UUID id,
            @NotNull long version,
            @NotNull UUID rechnungId,
            @NotNull String rechnungsnummer,
            @NotNull UUID kundeId,
            @NotNull String kundeName,
            @NotNull LocalDate faelligAm,

            @NotNull @Schema(description = "Tage seit der Fälligkeit, gerechnet bis heute")
            int tageUeberfaellig,

            @NotNull int stufe,
            @NotNull String bezeichnung,
            @NotNull LocalDate mahndatum,
            @NotNull LocalDate fristBis,
            @NotNull BigDecimal gebuehr,

            @NotNull @Schema(description = "ISO-Code, etwa CHF")
            String waehrung,

            @NotNull Mahnungsstatus status,

            @NotNull
            @Schema(
                    description =
                            "Betrag im Schreiben: bei Vorschlägen der heute offene Betrag plus Gebühr, danach der Stand"
                                    + " der Freigabe")
            BigDecimal zuZahlen,

            @NotNull @Schema(description = "Heute auf der Rechnung offen, ohne die Gebühr eines Vorschlags")
            BigDecimal aktuellOffen,

            Versandweg versandweg,
            String empfaengerEmail,
            Instant freigegebenAm,
            String freigegebenVon,
            Instant versendetAm,

            @Schema(description = "Meldung des Mailservers, wenn der Versand gescheitert ist")
            String versandfehler,

            Instant verworfenAm,
            String verworfenVon,
            String verworfenGrund,
            LocalDate gesperrtBis) {

        static MahnungDto von(MahnungZeile zeile, LocalDate heute) {
            return new MahnungDto(
                    zeile.id(),
                    zeile.version(),
                    zeile.rechnungId(),
                    zeile.rechnungsnummer(),
                    zeile.kundeId(),
                    zeile.kundeName(),
                    zeile.faelligAm(),
                    (int) Math.max(0, ChronoUnit.DAYS.between(zeile.faelligAm(), heute)),
                    zeile.stufe(),
                    zeile.bezeichnung(),
                    zeile.mahndatum(),
                    zeile.fristBis(),
                    zeile.gebuehr().betrag(),
                    zeile.gebuehr().waehrung().getCurrencyCode(),
                    zeile.status(),
                    zeile.zuZahlen().betrag(),
                    zeile.aktuellOffen().betrag(),
                    zeile.versandweg(),
                    zeile.empfaengerEmail(),
                    zeile.freigegebenAm(),
                    zeile.freigegebenVon(),
                    zeile.versendetAm(),
                    zeile.versandfehler(),
                    zeile.verworfenAm(),
                    zeile.verworfenVon(),
                    zeile.verworfenGrund(),
                    zeile.gesperrtBis());
        }
    }

    record MahnungAuswahlDto(@NotNull UUID id, @NotNull Long version) {}

    record FreigabeAnfrageDto(@NotEmpty @Size(max = 500) List<@Valid @NotNull MahnungAuswahlDto> mahnungen) {}

    record AblehnungDto(
            @NotNull UUID mahnungId,
            String rechnungsnummer,
            @NotNull String grund) {

        static AblehnungDto von(Ablehnung ablehnung) {
            return new AblehnungDto(ablehnung.mahnungId(), ablehnung.rechnungsnummer(), ablehnung.grund());
        }
    }

    record FreigabeErgebnisDto(
            @NotNull int freigegeben, @NotNull List<AblehnungDto> nichtFreigegeben) {

        static FreigabeErgebnisDto von(Freigabeergebnis ergebnis) {
            return new FreigabeErgebnisDto(
                    ergebnis.freigegeben(),
                    ergebnis.nichtFreigegeben().stream().map(AblehnungDto::von).toList());
        }
    }

    record VerwerfenDto(
            @NotNull Long version,
            @Size(max = 200) String grund,

            @Schema(description = "Bis zu diesem Tag schlägt der Mahnlauf die Rechnung nicht mehr vor")
            LocalDate gesperrtBis) {}

    record VersionDto(@NotNull Long version) {}

    record MahnlaufDto(
            @NotNull UUID id,
            @NotNull LocalDate stichtag,
            @NotNull Instant ausgefuehrtAm,
            @NotNull String ausgeloestVon,

            @NotNull @Schema(description = "Überfällige Rechnungen, die der Lauf geprüft hat")
            int geprueft,

            @NotNull int vorschlaege,

            @NotNull @Schema(description = "Alte Vorschläge zu Rechnungen, die nicht mehr offen sind")
            int verworfen) {

        static MahnlaufDto von(Mahnlauf lauf) {
            return new MahnlaufDto(
                    lauf.getId(),
                    lauf.getStichtag(),
                    lauf.getAusgefuehrtAm(),
                    lauf.getAusgeloestVon(),
                    lauf.getGeprueft(),
                    lauf.getVorschlaege(),
                    lauf.getVerworfen());
        }
    }

    record MahnstufeDto(
            @NotNull UUID id,
            @NotNull long version,
            @NotNull int stufe,
            @NotNull String bezeichnung,
            @NotNull int tageNachFaelligkeit,
            @NotNull int mindestabstandTage,
            @NotNull int fristTage,
            @NotNull BigDecimal gebuehr,
            @NotNull String textvorlage) {

        static MahnstufeDto von(Mahnstufe stufe) {
            return new MahnstufeDto(
                    stufe.getId(),
                    stufe.getVersion() == null ? 0 : stufe.getVersion(),
                    stufe.getStufe(),
                    stufe.getBezeichnung(),
                    stufe.getTageNachFaelligkeit(),
                    stufe.getMindestabstandTage(),
                    stufe.getFristTage(),
                    stufe.getGebuehr(),
                    stufe.getTextvorlage());
        }
    }

    record MahnstufeEingabeDto(
            @Schema(description = "Version der bisherigen Stufe an dieser Position; leer für eine neue Stufe")
            Long version,

            @NotBlank @Size(max = 40) String bezeichnung,
            @NotNull @Min(1) @Max(365) Integer tageNachFaelligkeit,

            @NotNull @Min(0) @Max(365) @Schema(description = "Abstand zur vorherigen Stufe; bei Stufe 1 ohne Wirkung")
            Integer mindestabstandTage,

            @NotNull @Min(1) @Max(90) Integer fristTage,

            @NotNull @DecimalMin("0.00") @DecimalMax("1000.00") @Digits(integer = 4, fraction = 2)
            BigDecimal gebuehr,

            @NotBlank
            @Size(max = 2000)
            @Schema(
                    description =
                            "Brieftext; Platzhalter {rechnungsnummer}, {faelligAm}, {offen}, {frist} und {gebuehr}")
            String textvorlage) {

        Mahnstufendaten alsDaten() {
            return new Mahnstufendaten(
                    bezeichnung, tageNachFaelligkeit, mindestabstandTage, fristTage, gebuehr, textvorlage);
        }
    }

    record MahnstufenAnfrageDto(@NotEmpty @Size(max = 5) List<@Valid @NotNull MahnstufeEingabeDto> stufen) {}
}
