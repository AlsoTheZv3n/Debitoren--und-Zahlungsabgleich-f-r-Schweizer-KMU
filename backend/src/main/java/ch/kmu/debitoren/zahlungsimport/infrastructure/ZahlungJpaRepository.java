package ch.kmu.debitoren.zahlungsimport.infrastructure;

import ch.kmu.debitoren.zahlungsimport.domain.Zahlung;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungRepository;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link ZahlungRepository}; Hibernate filtert nach Mandant. */
interface ZahlungJpaRepository extends JpaRepository<Zahlung, UUID>, ZahlungRepository {

    /** PostgreSQL erlaubt viele Parameter, aber lange IN-Listen werden langsam. */
    int BLOCKGROESSE = 1000;

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    long countByStatus(Zahlungsstatus status);

    @Override
    default long anzahlMitStatus(Zahlungsstatus status) {
        return countByStatus(status);
    }

    @Query("select z from Zahlung z where z.id = :id")
    Optional<Zahlung> mitVersionserhoehung(UUID id);

    @Query("select z.bankReferenz from Zahlung z where z.bankReferenz in :bankReferenzen")
    List<String> vorhandeneBankreferenzen(Collection<String> bankReferenzen);

    @Override
    default void speichernAlle(List<Zahlung> zahlungen) {
        saveAllAndFlush(zahlungen);
    }

    @Override
    default Optional<Zahlung> finde(UUID id) {
        return findById(id);
    }

    @Override
    default Optional<Zahlung> zumAendern(UUID id) {
        return mitVersionserhoehung(id);
    }

    @Override
    default Set<String> bekannteBankreferenzen(Collection<String> bankReferenzen) {
        var liste = new ArrayList<>(bankReferenzen);
        var bekannt = new HashSet<String>();
        for (int start = 0; start < liste.size(); start += BLOCKGROESSE) {
            bekannt.addAll(
                    vorhandeneBankreferenzen(liste.subList(start, Math.min(start + BLOCKGROESSE, liste.size()))));
        }
        return bekannt;
    }
}
