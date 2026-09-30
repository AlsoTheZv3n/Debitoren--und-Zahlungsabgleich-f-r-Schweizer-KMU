package ch.kmu.debitoren.stammdaten.infrastructure;

import ch.kmu.debitoren.stammdaten.domain.Mandant;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link MandantRepository}. */
interface MandantJpaRepository extends JpaRepository<Mandant, UUID>, MandantRepository {

    @Override
    @Query("select m from Mandant m where m.id in :ids order by m.name")
    List<Mandant> findeAlle(Collection<UUID> ids);

    @Override
    default Optional<Mandant> finde(UUID id) {
        return findById(id);
    }

    @Override
    @Query("select m.id from Mandant m order by m.name")
    List<UUID> alleIds();

    @Override
    default boolean existiert(UUID id) {
        return existsById(id);
    }

    @Override
    default Mandant speichern(Mandant mandant) {
        return save(mandant);
    }
}
