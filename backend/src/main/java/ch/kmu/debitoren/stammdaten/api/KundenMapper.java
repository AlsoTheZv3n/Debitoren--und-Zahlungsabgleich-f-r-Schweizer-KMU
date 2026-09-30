package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.stammdaten.application.KundeDaten;
import ch.kmu.debitoren.stammdaten.domain.Kunde;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface KundenMapper {

    @Mapping(target = "strasse", source = "adresse.strasse")
    @Mapping(target = "hausnummer", source = "adresse.hausnummer")
    @Mapping(target = "plz", source = "adresse.plz")
    @Mapping(target = "ort", source = "adresse.ort")
    @Mapping(target = "land", source = "adresse.land")
    KundeDto zuDto(Kunde kunde);

    default KundeDaten zuDaten(KundeEingabeDto eingabe) {
        return new KundeDaten(
                eingabe.kundennummer(),
                eingabe.name(),
                new Adresse(eingabe.strasse(), eingabe.hausnummer(), eingabe.plz(), eingabe.ort(), eingabe.land()),
                eingabe.email(),
                eingabe.zahlungsfristTage());
    }

    default KundeDaten zuDaten(KundeAenderungDto aenderung) {
        return new KundeDaten(
                null,
                aenderung.name(),
                new Adresse(
                        aenderung.strasse(),
                        aenderung.hausnummer(),
                        aenderung.plz(),
                        aenderung.ort(),
                        aenderung.land()),
                aenderung.email(),
                aenderung.zahlungsfristTage());
    }
}
