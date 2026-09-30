package ch.kmu.debitoren.rechnung.api;

import ch.kmu.debitoren.rechnung.api.RechnungDtos.EmpfaengerDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.EntwurfDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.EntwurfspositionDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.MwstAnteilDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.MwstSatzDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.PositionDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.RechnungDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.RechnungZeileDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.SummenDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.VorschauAnfrageDto;
import ch.kmu.debitoren.rechnung.api.RechnungDtos.Waehrung;
import ch.kmu.debitoren.rechnung.application.Entwurf;
import ch.kmu.debitoren.rechnung.domain.MwstSatz;
import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungZeile;
import ch.kmu.debitoren.rechnung.domain.Rechnungsposition;
import ch.kmu.debitoren.rechnung.domain.Summen;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Abbildung zwischen API und Use Cases. Einfache Felder übernimmt MapStruct; Geld, Währung und abgeleitete
 * Angaben wie «überfällig» stehen als Methoden hier.
 */
@Mapper
interface RechnungAbbildung {

    PositionDto zuDto(Rechnungsposition position);

    MwstSatzDto zuDto(MwstSatz satz);

    List<MwstSatzDto> zuSatzDtos(List<MwstSatz> saetze);

    @Mapping(target = "ueberfaellig", expression = "java(zeile.istUeberfaellig(heute))")
    RechnungZeileDto zuDto(RechnungZeile zeile, @org.mapstruct.Context LocalDate heute);

    default RechnungDto zuDto(Rechnung rechnung, LocalDate heute, BigDecimal offen) {
        var empfaenger = rechnung.getEmpfaenger();
        var adresse = empfaenger.adresse();
        return new RechnungDto(
                rechnung.getId(),
                rechnung.getVersion() == null ? 0 : rechnung.getVersion(),
                rechnung.getStatus(),
                rechnung.istUeberfaellig(heute),
                rechnung.getRechnungsnummer(),
                rechnung.getReferenz() == null ? null : rechnung.getReferenz().formatiert(),
                rechnung.getReferenz() == null ? null : rechnung.getReferenz().art(),
                rechnung.getKundeId(),
                new EmpfaengerDto(
                        empfaenger.name(),
                        adresse.strasse(),
                        adresse.hausnummer(),
                        adresse.plz(),
                        adresse.ort(),
                        adresse.land()),
                rechnung.getRechnungsdatum(),
                rechnung.getZahlungsfristTage(),
                rechnung.getFaelligAm(),
                waehrung(rechnung.getWaehrung()),
                rechnung.getBemerkung(),
                rechnung.getPositionen().stream().map(this::zuDto).toList(),
                zuDto(rechnung.summen()),
                rechnung.getMahngebuehren().betrag(),
                offen,
                rechnung.getFreigegebenAm(),
                rechnung.getStorniertAm(),
                rechnung.getStornoGrund());
    }

    default SummenDto zuDto(Summen summen) {
        return new SummenDto(
                summen.netto().betrag(),
                summen.mwst().stream()
                        .map(anteil -> new MwstAnteilDto(
                                anteil.code(),
                                anteil.prozent(),
                                anteil.basis().betrag(),
                                anteil.betrag().betrag()))
                        .toList(),
                summen.mwstTotal().betrag(),
                summen.rundung().betrag(),
                summen.total().betrag());
    }

    default Entwurf zuEntwurf(EntwurfDto dto) {
        return new Entwurf(
                dto.kundeId(),
                dto.rechnungsdatum(),
                dto.zahlungsfristTage(),
                waehrung(dto.waehrung()),
                dto.bemerkung(),
                positionen(dto.positionen()));
    }

    default Entwurf zuEntwurf(VorschauAnfrageDto dto) {
        return new Entwurf(
                dto.kundeId(),
                dto.rechnungsdatum(),
                dto.zahlungsfristTage(),
                waehrung(dto.waehrung()),
                null,
                positionen(dto.positionen()));
    }

    private List<Entwurf.Position> positionen(List<EntwurfspositionDto> positionen) {
        return positionen.stream()
                .map(position -> new Entwurf.Position(
                        position.beschreibung(),
                        position.menge(),
                        position.einheit(),
                        position.einzelpreis(),
                        position.mwstCode()))
                .toList();
    }

    default Waehrung waehrung(Currency waehrung) {
        return Waehrung.valueOf(waehrung.getCurrencyCode());
    }

    default Currency waehrung(Waehrung waehrung) {
        return Currency.getInstance(waehrung.name());
    }
}
