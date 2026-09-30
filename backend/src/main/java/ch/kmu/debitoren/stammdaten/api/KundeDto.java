package ch.kmu.debitoren.stammdaten.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record KundeDto(
        @NotNull UUID id,
        @NotNull String kundennummer,
        @NotNull String name,
        String strasse,
        String hausnummer,
        @NotNull String plz,
        @NotNull String ort,
        @NotNull String land,
        String email,
        @NotNull int zahlungsfristTage,
        @NotNull long version) {}
