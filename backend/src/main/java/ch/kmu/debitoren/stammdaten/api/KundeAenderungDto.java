package ch.kmu.debitoren.stammdaten.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Änderung eines Kunden; {@code version} ist der Stand, den der Benutzer bearbeitet hat. */
record KundeAenderungDto(
        @NotBlank @Size(max = 70) String name,
        @Size(max = 70) String strasse,
        @Size(max = 16) String hausnummer,
        @NotBlank @Size(max = 16) String plz,
        @NotBlank @Size(max = 35) String ort,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String land,
        @Email @Size(max = 254) String email,
        @NotNull @Min(0) @Max(365) Integer zahlungsfristTage,
        @NotNull Long version) {}
