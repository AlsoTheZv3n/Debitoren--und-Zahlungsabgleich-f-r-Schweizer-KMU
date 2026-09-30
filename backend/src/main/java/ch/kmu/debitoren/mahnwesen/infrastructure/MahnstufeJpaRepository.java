package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.Mahnstufe;
import ch.kmu.debitoren.mahnwesen.domain.MahnstufeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Adapter: Spring Data implementiert den Port {@link MahnstufeRepository}; Hibernate filtert nach Mandant. */
interface MahnstufeJpaRepository extends JpaRepository<Mahnstufe, UUID>, MahnstufeRepository {

    List<Mahnstufe> findAllByOrderByStufe();

    @Override
    default List<Mahnstufe> alle() {
        return findAllByOrderByStufe();
    }

    @Override
    default boolean vorhanden() {
        return count() > 0;
    }

    @Override
    default Mahnstufe speichern(Mahnstufe stufe) {
        return save(stufe);
    }

    @Override
    default void loeschen(Mahnstufe stufe) {
        delete(stufe);
    }
}
