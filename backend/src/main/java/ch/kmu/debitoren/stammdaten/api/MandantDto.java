package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.shared.domain.Referenzart;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record MandantDto(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull String ort,
        @NotNull Referenzart referenzart) {}
