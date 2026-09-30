package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.Mahnung;
import ch.kmu.debitoren.mahnwesen.domain.MahnungRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link MahnungRepository}; Hibernate filtert nach Mandant. */
interface MahnungJpaRepository extends JpaRepository<Mahnung, UUID>, MahnungRepository {

    /** Grösse der Blöcke für {@code in}-Listen; PostgreSQL erlaubt viele, der Plan bleibt aber so übersichtlich. */
    int BLOCK = 500;

    List<Mahnung> findByRechnungIdIn(Collection<UUID> rechnungIds);

    List<Mahnung> findByStatusOrderByMahndatum(Mahnungsstatus status);

    @Modifying
    @Query(value = "update mahnung set pdf_schluessel = :schluessel where id = :id", nativeQuery = true)
    int pdfSchluesselSetzen(UUID id, String schluessel);

    @Override
    default Mahnung speichern(Mahnung mahnung) {
        return save(mahnung);
    }

    @Override
    default Optional<Mahnung> finde(UUID id) {
        return findById(id);
    }

    @Override
    default List<Mahnung> fuerRechnungen(Collection<UUID> rechnungIds) {
        var ids = List.copyOf(rechnungIds);
        var alle = new ArrayList<Mahnung>();
        for (int von = 0; von < ids.size(); von += BLOCK) {
            alle.addAll(findByRechnungIdIn(ids.subList(von, Math.min(von + BLOCK, ids.size()))));
        }
        return alle;
    }

    @Override
    default List<Mahnung> vorschlaege() {
        return findByStatusOrderByMahndatum(Mahnungsstatus.VORSCHLAG);
    }

    @Override
    default void pdfAbgelegt(UUID id, String schluessel) {
        pdfSchluesselSetzen(id, schluessel);
    }
}
