package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.stammdaten.application.MandantDaten;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Änderung des aktuellen Mandanten; {@code version} ist der Stand, den der Admin bearbeitet hat. */
record MandantAenderungDto(
        @NotBlank @Size(max = 70) String name,
        @Size(max = 70) String strasse,
        @Size(max = 16) String hausnummer,
        @NotBlank @Size(max = 16) String plz,
        @NotBlank @Size(max = 35) String ort,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String land,

        @NotBlank @Size(max = 34) @Schema(description = "IBAN ohne QR-IBAN; Leerzeichen sind erlaubt")
        String iban,

        @Size(max = 34) @Schema(description = "Optional; damit erhalten neue Rechnungen eine QR-Referenz")
        String qrIban,

        @Size(max = 20) @Schema(description = "Format CHE-123.456.789 MWST")
        String mwstNummer,

        @Email @Size(max = 254) String email,
        @NotNull Boolean rundungFuenfRappen,
        @NotNull Long version) {

    MandantDaten alsDaten() {
        return new MandantDaten(
                name,
                new Adresse(strasse, hausnummer, plz, ort, land),
                new Iban(iban),
                qrIban == null || qrIban.isBlank() ? null : new Iban(qrIban),
                mwstNummer,
                email,
                rundungFuenfRappen);
    }
}
