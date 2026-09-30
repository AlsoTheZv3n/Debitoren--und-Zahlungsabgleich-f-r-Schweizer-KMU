package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.Mahnlauf;
import ch.kmu.debitoren.mahnwesen.domain.MahnlaufRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Adapter: Spring Data implementiert den Port {@link MahnlaufRepository}; Hibernate filtert nach Mandant. */
interface MahnlaufJpaRepository extends JpaRepository<Mahnlauf, UUID>, MahnlaufRepository {

    List<Mahnlauf> findByOrderByAusgefuehrtAmDesc(Pageable seite);

    @Override
    default Mahnlauf speichern(Mahnlauf lauf) {
        return save(lauf);
    }

    @Override
    default List<Mahnlauf> letzte(int anzahl) {
        return findByOrderByAusgefuehrtAmDesc(PageRequest.of(0, anzahl));
    }
}
