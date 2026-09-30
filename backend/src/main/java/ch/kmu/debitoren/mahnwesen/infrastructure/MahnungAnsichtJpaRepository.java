package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.MahnungZeile;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsuebersicht;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter für den Port {@link Mahnungsuebersicht} auf der Sicht {@code v_mahnungen}. */
interface MahnungAnsichtJpaRepository extends JpaRepository<MahnungAnsicht, UUID>, Mahnungsuebersicht {

    /**
     * Vorschläge nach Fälligkeit, damit die ältesten Rechnungen oben stehen; alles andere nach Mahndatum, neueste
     * zuerst.
     */
    String FILTER = """
            where (:status is null or m.status = :status)
              and (:rechnungId is null or m.rechnungId = :rechnungId)
              and (:muster is null
                   or lower(m.rechnungsnummer) like :muster
                   or lower(m.kundeName) like :muster)
            """;

    @Query(
            value = "select m from MahnungAnsicht m " + FILTER + """
                    order by
                      case when m.status = ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus.VORSCHLAG
                           then m.faelligAm end asc,
                      m.mahndatum desc, m.erstelltAm desc, m.mahnungId desc
                    """,
            countQuery = "select count(m) from MahnungAnsicht m " + FILTER)
    Page<MahnungAnsicht> sucheMitFilter(Mahnungsstatus status, UUID rechnungId, String muster, Pageable seite);

    List<MahnungAnsicht> findByRechnungIdOrderByStufeAscErstelltAmAsc(UUID rechnungId);

    @Override
    default Page<MahnungZeile> suche(Mahnungsstatus status, UUID rechnungId, String suchtext, Pageable seite) {
        var muster = suchtext == null || suchtext.isBlank()
                ? null
                : "%"
                        + suchtext.strip()
                                .toLowerCase(Locale.ROOT)
                                .replace("%", "")
                                .replace("_", "") + "%";
        return sucheMitFilter(status, rechnungId, muster, seite).map(MahnungAnsicht::alsZeile);
    }

    @Override
    default Optional<MahnungZeile> zeile(UUID id) {
        return findById(id).map(MahnungAnsicht::alsZeile);
    }

    @Override
    default List<MahnungZeile> derRechnung(UUID rechnungId) {
        return findByRechnungIdOrderByStufeAscErstelltAmAsc(rechnungId).stream()
                .map(MahnungAnsicht::alsZeile)
                .toList();
    }
}
