package ch.kmu.debitoren.stammdaten.api;

import ch.kmu.debitoren.stammdaten.domain.Mandant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface MandantMapper {

    @Mapping(target = "ort", source = "adresse.ort")
    MandantDto zuDto(Mandant mandant);

    List<MandantDto> zuDtos(List<Mandant> mandanten);
}
