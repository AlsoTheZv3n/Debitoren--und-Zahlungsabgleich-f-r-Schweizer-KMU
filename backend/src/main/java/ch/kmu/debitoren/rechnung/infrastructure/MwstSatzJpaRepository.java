package ch.kmu.debitoren.rechnung.infrastructure;

import ch.kmu.debitoren.rechnung.domain.MwstCode;
import ch.kmu.debitoren.rechnung.domain.MwstSatz;
import ch.kmu.debitoren.rechnung.domain.MwstSatzRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link MwstSatzRepository}. */
interface MwstSatzJpaRepository extends JpaRepository<MwstSatz, UUID>, MwstSatzRepository {

    @Query("select s from MwstSatz s order by s.code, s.gueltigAb")
    List<MwstSatz> alleSortiert();

    @Query("""
            select s from MwstSatz s
            where s.code = :code and s.gueltigAb <= :datum and (s.gueltigBis is null or s.gueltigBis >= :datum)
            order by s.gueltigAb desc
            """)
    List<MwstSatz> gueltigeAm(MwstCode code, LocalDate datum);

    @Override
    default List<MwstSatz> alle() {
        return alleSortiert();
    }

    @Override
    default Optional<MwstSatz> gueltigAm(MwstCode code, LocalDate datum) {
        return gueltigeAm(code, datum).stream().findFirst();
    }

    @Override
    default boolean vorhanden() {
        return count() > 0;
    }

    @Override
    default MwstSatz speichern(MwstSatz satz) {
        return save(satz);
    }
}
