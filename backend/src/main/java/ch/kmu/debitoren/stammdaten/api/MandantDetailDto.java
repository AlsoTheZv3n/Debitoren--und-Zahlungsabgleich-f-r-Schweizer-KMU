package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.shared.domain.Referenzart;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Alle Angaben des aktuellen Mandanten, für die Einstellungen. */
record MandantDetailDto(
        @NotNull UUID id,
        @NotNull long version,
        @NotNull String name,
        String strasse,
        String hausnummer,
        @NotNull String plz,
        @NotNull String ort,
        @NotNull String land,
        @NotNull String iban,
        String qrIban,
        String mwstNummer,
        String email,
        @NotNull boolean rundungFuenfRappen,

        @NotNull @Schema(description = "QR mit QR-IBAN, sonst SCOR")
        Referenzart referenzart) {

    static MandantDetailDto von(Mandant mandant) {
        var adresse = mandant.getAdresse();
        return new MandantDetailDto(
                mandant.getId(),
                mandant.getVersion() == null ? 0 : mandant.getVersion(),
                mandant.getName(),
                adresse.strasse(),
                adresse.hausnummer(),
                adresse.plz(),
                adresse.ort(),
                adresse.land(),
                mandant.getIban().wert(),
                mandant.getQrIban() == null ? null : mandant.getQrIban().wert(),
                mandant.getMwstNummer(),
                mandant.getEmail(),
                mandant.isRundungFuenfRappen(),
                mandant.getReferenzart());
    }
}
