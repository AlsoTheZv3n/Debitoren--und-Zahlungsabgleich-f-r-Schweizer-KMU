package ch.kmu.debitoren.abgleich.infrastructure;

import ch.kmu.debitoren.abgleich.domain.Zuordnung;
import ch.kmu.debitoren.abgleich.domain.ZuordnungRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Adapter: Spring Data implementiert den Port {@link ZuordnungRepository}; Hibernate filtert nach Mandant. */
interface ZuordnungJpaRepository extends JpaRepository<Zuordnung, UUID>, ZuordnungRepository {

    List<Zuordnung> findByZahlungIdOrderByErstelltAm(UUID zahlungId);

    List<Zuordnung> findByRechnungIdOrderByErstelltAm(UUID rechnungId);

    @Override
    default Zuordnung speichern(Zuordnung zuordnung) {
        return save(zuordnung);
    }

    @Override
    default Optional<Zuordnung> finde(UUID id) {
        return findById(id);
    }

    @Override
    default void loeschen(Zuordnung zuordnung) {
        delete(zuordnung);
        flush();
    }

    @Override
    default List<Zuordnung> fuerZahlung(UUID zahlungId) {
        return findByZahlungIdOrderByErstelltAm(zahlungId);
    }

    @Override
    default List<Zuordnung> fuerRechnung(UUID rechnungId) {
        return findByRechnungIdOrderByErstelltAm(rechnungId);
    }
}
