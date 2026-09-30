package ch.kmu.debitoren.rechnung.api;

import ch.kmu.debitoren.rechnung.domain.MwstCode;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.shared.domain.Referenzart;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Datenformate der Rechnungs-API. Beträge sind Zahlen mit zwei Nachkommastellen. */
final class RechnungDtos {

    private RechnungDtos() {}

    /** Der QR-Zahlteil erlaubt nur diese beiden Währungen. */
    enum Waehrung {
        CHF,
        EUR
    }

    enum Rechnungssortierung {
        DATUM_ABSTEIGEND,
        DATUM_AUFSTEIGEND,
        FAELLIGKEIT_AUFSTEIGEND,
        NUMMER_ABSTEIGEND,
        TOTAL_ABSTEIGEND
    }

    record RechnungZeileDto(
            @NotNull UUID id,
            @NotNull long version,
            String rechnungsnummer,
            @NotNull Rechnungsstatus status,
            @NotNull boolean ueberfaellig,
            @NotNull UUID kundeId,
            @NotNull String kundeName,
            @NotNull LocalDate rechnungsdatum,
            @NotNull LocalDate faelligAm,
            @NotNull Waehrung waehrung,
            @NotNull BigDecimal total,
            @NotNull BigDecimal offen) {}

    record RechnungDto(
            @NotNull UUID id,
            @NotNull long version,
            @NotNull Rechnungsstatus status,
            @NotNull boolean ueberfaellig,
            String rechnungsnummer,
            String referenz,
            Referenzart referenzart,
            @NotNull UUID kundeId,
            @NotNull EmpfaengerDto empfaenger,
            @NotNull LocalDate rechnungsdatum,
            @NotNull int zahlungsfristTage,
            @NotNull LocalDate faelligAm,
            @NotNull Waehrung waehrung,
            String bemerkung,
            @NotNull List<PositionDto> positionen,
            @NotNull SummenDto summen,

            @NotNull
            @Schema(
                    description =
                            "Total abzüglich Zahlungseingänge; 0 bei Entwürfen, bezahlten und stornierten Rechnungen")
            BigDecimal offen,

            Instant freigegebenAm,
            Instant storniertAm,
            String stornoGrund) {}

    record EmpfaengerDto(
            @NotNull String name,
            String strasse,
            String hausnummer,
            @NotNull String plz,
            @NotNull String ort,
            @NotNull String land) {}

    record PositionDto(
            @NotNull int position,
            @NotNull String beschreibung,
            @NotNull BigDecimal menge,
            String einheit,
            @NotNull BigDecimal einzelpreis,
            @NotNull MwstCode mwstCode,
            @NotNull BigDecimal mwstProzent,
            @NotNull BigDecimal betrag) {}

    record SummenDto(
            @NotNull BigDecimal netto,
            @NotNull List<MwstAnteilDto> mwst,
            @NotNull BigDecimal mwstTotal,
            @NotNull BigDecimal rundung,
            @NotNull BigDecimal total) {}

    record MwstAnteilDto(
            @NotNull MwstCode code,
            @NotNull BigDecimal prozent,
            @NotNull BigDecimal basis,
            @NotNull BigDecimal betrag) {}

    /** Entwurf; ohne Zahlungsfrist gilt die des Kunden. */
    record EntwurfDto(
            @NotNull UUID kundeId,
            @NotNull LocalDate rechnungsdatum,
            @Min(0) @Max(365) Integer zahlungsfristTage,
            @NotNull Waehrung waehrung,
            @Size(max = 500) String bemerkung,
            @NotNull @Size(max = 100) List<@Valid @NotNull EntwurfspositionDto> positionen) {}

    record EntwurfspositionDto(
            @NotBlank @Size(max = 200) String beschreibung,

            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
            BigDecimal menge,

            @Size(max = 10) String einheit,

            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
            BigDecimal einzelpreis,

            @NotNull MwstCode mwstCode) {}

    record EntwurfAenderungDto(
            @NotNull Long version, @NotNull @Valid EntwurfDto entwurf) {}

    /** Für die Live-Vorschau; der Kunde darf noch fehlen. */
    record VorschauAnfrageDto(
            UUID kundeId,
            @NotNull LocalDate rechnungsdatum,
            @Min(0) @Max(365) Integer zahlungsfristTage,
            @NotNull Waehrung waehrung,
            @NotNull @Size(max = 100) List<@Valid @NotNull EntwurfspositionDto> positionen) {}

    record VorschauDto(
            @NotNull SummenDto summen,
            @NotNull List<BigDecimal> positionsbetraege,
            @NotNull String zahlteilSvg) {}

    record VersionDto(@NotNull Long version) {}

    record StornoDto(
            @NotNull Long version,
            @NotBlank @Size(max = 200) String grund) {}

    record MwstSatzDto(
            @NotNull UUID id,
            @NotNull MwstCode code,
            @NotNull String bezeichnung,
            @NotNull BigDecimal satz,
            @NotNull LocalDate gueltigAb,
            LocalDate gueltigBis) {}
}
