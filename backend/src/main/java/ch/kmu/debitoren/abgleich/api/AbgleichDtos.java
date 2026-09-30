package ch.kmu.debitoren.abgleich.api;

import ch.kmu.debitoren.abgleich.application.ZuordnungZeile;
import ch.kmu.debitoren.abgleich.domain.ZahlungZeile;
import ch.kmu.debitoren.abgleich.domain.ZuordnungsVorschlag;
import ch.kmu.debitoren.abgleich.domain.Zuordnungsart;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Datenformate der API für Zahlungen, Vorschläge und Zuordnungen. */
final class AbgleichDtos {

    private AbgleichDtos() {}

    record ZahlungDto(
            @NotNull UUID id,
            @NotNull long version,
            @NotNull UUID zahlungsdateiId,
            @NotNull String bankReferenz,
            LocalDate buchungsdatum,
            @NotNull LocalDate valuta,
            @NotNull BigDecimal betrag,

            @NotNull @Schema(description = "Noch keiner Rechnung zugeordnet")
            BigDecimal rest,

            @NotNull @Schema(description = "ISO-Code, etwa CHF")
            String waehrung,

            @Schema(description = "QR- oder SCOR-Referenz ohne Leerzeichen")
            String referenz,

            String auftraggeber,
            String auftraggeberIban,
            String mitteilung,
            @NotNull Zahlungsstatus status,
            Klaerungsgrund klaerungsgrund) {

        static ZahlungDto von(ZahlungZeile zeile) {
            return new ZahlungDto(
                    zeile.id(),
                    zeile.version(),
                    zeile.zahlungsdateiId(),
                    zeile.bankReferenz(),
                    zeile.buchungsdatum(),
                    zeile.valuta(),
                    zeile.betrag().betrag(),
                    zeile.rest().betrag(),
                    zeile.betrag().waehrung().getCurrencyCode(),
                    zeile.referenz(),
                    zeile.auftraggeber(),
                    zeile.auftraggeberIban(),
                    zeile.mitteilung(),
                    zeile.status(),
                    zeile.klaerungsgrund());
        }
    }

    record VorschlagDto(
            @NotNull UUID rechnungId,
            @NotNull String rechnungsnummer,
            @NotNull String kundeName,
            @NotNull LocalDate faelligAm,
            @NotNull BigDecimal offen,

            @NotNull @Schema(description = "Was zugeordnet würde")
            BigDecimal betrag,

            @NotNull String waehrung,
            @NotNull String regel,
            @NotNull boolean automatisch,
            @NotNull String begruendung) {

        static VorschlagDto von(ZuordnungsVorschlag vorschlag) {
            var posten = vorschlag.posten();
            return new VorschlagDto(
                    posten.rechnungId(),
                    posten.rechnungsnummer(),
                    posten.kundeName(),
                    posten.faelligAm(),
                    posten.offen().betrag(),
                    vorschlag.betrag().betrag(),
                    vorschlag.betrag().waehrung().getCurrencyCode(),
                    vorschlag.regel(),
                    vorschlag.automatisch(),
                    vorschlag.begruendung());
        }
    }

    record ZuordnungAnfrageDto(
            @NotNull @Schema(description = "Version der Zahlung, wie sie angezeigt wurde")
            Long version,

            @NotNull UUID rechnungId,

            @DecimalMin(value = "0.01")
            @Digits(integer = 10, fraction = 2)
            @Schema(description = "Ohne Angabe: Rest der Zahlung, höchstens der offene Betrag")
            BigDecimal betrag) {}

    record ZuordnungDto(
            @NotNull UUID id,
            @NotNull UUID zahlungId,
            @NotNull UUID rechnungId,
            String rechnungsnummer,
            String kundeName,
            @NotNull BigDecimal betrag,
            @NotNull String waehrung,
            @NotNull Zuordnungsart art,
            @NotNull String regel,
            @NotNull String erstelltVon,
            @NotNull Instant erstelltAm,
            LocalDate valuta,
            String auftraggeber) {

        static ZuordnungDto von(ZuordnungZeile zeile) {
            return new ZuordnungDto(
                    zeile.id(),
                    zeile.zahlungId(),
                    zeile.rechnungId(),
                    zeile.rechnungsnummer(),
                    zeile.kundeName(),
                    zeile.betrag().betrag(),
                    zeile.betrag().waehrung().getCurrencyCode(),
                    zeile.art(),
                    zeile.regel(),
                    zeile.erstelltVon(),
                    zeile.erstelltAm(),
                    zeile.valuta(),
                    zeile.auftraggeber());
        }
    }
}
