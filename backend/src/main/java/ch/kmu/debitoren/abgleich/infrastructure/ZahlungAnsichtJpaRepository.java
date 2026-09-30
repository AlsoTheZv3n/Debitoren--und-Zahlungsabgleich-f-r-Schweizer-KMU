package ch.kmu.debitoren.abgleich.infrastructure;

import ch.kmu.debitoren.abgleich.domain.ZahlungZeile;
import ch.kmu.debitoren.abgleich.domain.Zahlungsuebersicht;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter für den Port {@link Zahlungsuebersicht} auf der Sicht {@code v_zahlungen}. */
interface ZahlungAnsichtJpaRepository extends JpaRepository<ZahlungAnsicht, UUID>, Zahlungsuebersicht {

    @Query("""
            select z from ZahlungAnsicht z
            where (:status is null or z.status = :status)
              and (:zahlungsdateiId is null or z.zahlungsdateiId = :zahlungsdateiId)
              and (:muster is null
                   or lower(z.auftraggeber) like :muster
                   or lower(z.referenz) like :muster
                   or lower(z.mitteilung) like :muster
                   or lower(z.bankReferenz) like :muster)
            order by z.valuta desc, z.zahlungId desc
            """)
    Page<ZahlungAnsicht> sucheMitFilter(Zahlungsstatus status, UUID zahlungsdateiId, String muster, Pageable seite);

    @Override
    default Page<ZahlungZeile> suche(Zahlungsstatus status, UUID zahlungsdateiId, String suchtext, Pageable seite) {
        var muster = suchtext == null || suchtext.isBlank()
                ? null
                : "%"
                        + suchtext.strip()
                                .toLowerCase(Locale.ROOT)
                                .replace("%", "")
                                .replace("_", "") + "%";
        return sucheMitFilter(status, zahlungsdateiId, muster, seite).map(ZahlungAnsicht::alsZeile);
    }

    @Override
    default Optional<ZahlungZeile> zeile(UUID id) {
        return findById(id).map(ZahlungAnsicht::alsZeile);
    }
}
