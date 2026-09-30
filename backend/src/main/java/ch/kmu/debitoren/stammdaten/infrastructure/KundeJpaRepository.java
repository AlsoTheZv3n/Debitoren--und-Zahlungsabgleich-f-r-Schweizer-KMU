package ch.kmu.debitoren.stammdaten.infrastructure;

import ch.kmu.debitoren.stammdaten.domain.Kunde;
import ch.kmu.debitoren.stammdaten.domain.KundeRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link KundeRepository}; Hibernate filtert nach Mandant. */
interface KundeJpaRepository extends JpaRepository<Kunde, UUID>, KundeRepository {

    Optional<Kunde> findByKundennummer(String kundennummer);

    @Query("""
            select k from Kunde k
            where lower(k.name) like :muster
               or lower(k.kundennummer) like :muster
               or lower(k.adresse.ort) like :muster
            """)
    Page<Kunde> sucheNachMuster(String muster, Pageable seite);

    @Override
    default Kunde speichern(Kunde kunde) {
        return save(kunde);
    }

    @Override
    default Optional<Kunde> finde(UUID id) {
        return findById(id);
    }

    @Override
    default Optional<Kunde> findeNachKundennummer(String kundennummer) {
        return findByKundennummer(kundennummer);
    }

    @Override
    default Page<Kunde> suche(String suchtext, Pageable seite) {
        if (suchtext == null || suchtext.isBlank()) {
            return findAll(seite);
        }
        var maskiert =
                suchtext.strip().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return sucheNachMuster("%" + maskiert + "%", seite);
    }

    @Override
    default long anzahl() {
        return count();
    }
}
