package ch.kmu.debitoren.shared.security;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

record BenutzerDto(
        @NotNull String benutzername,
        @NotNull String anzeigename,
        String email,
        @NotNull List<Rolle> rollen,
        @NotNull List<UUID> mandanten) {}
