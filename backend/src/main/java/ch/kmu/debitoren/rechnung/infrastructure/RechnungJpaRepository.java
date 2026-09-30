package ch.kmu.debitoren.rechnung.infrastructure;

import ch.kmu.debitoren.rechnung.domain.Posten;
import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungRepository;
import ch.kmu.debitoren.rechnung.domain.RechnungZeile;
import ch.kmu.debitoren.rechnung.domain.Rechnungsfilter;
import ch.kmu.debitoren.rechnung.domain.Rechnungsstatus;
import ch.kmu.debitoren.rechnung.domain.Zahlungsreferenz;
import ch.kmu.debitoren.shared.domain.Geld;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link RechnungRepository}; Hibernate filtert nach Mandant. */
interface RechnungJpaRepository extends JpaRepository<Rechnung, UUID>, RechnungRepository {

    String FILTER = """
            where (:status is null or r.status = :status)
              and (:kundeId is null or r.kundeId = :kundeId)
              and (:nurOffene = false or r.status in :offene)
              and (:nurUeberfaellige = false or (r.status in :offene and r.faelligAm < :heute))
              and (:faelligBis is null or r.faelligAm <= :faelligBis)
              and (:muster is null
                   or lower(r.rechnungsnummer) like :muster
                   or lower(r.empfaengerName) like :muster)
            """;

    @Query(value = """
                    select new ch.kmu.debitoren.rechnung.domain.RechnungZeile(
                        r.id, r.version, r.rechnungsnummer, r.status, r.kundeId, r.empfaengerName, r.rechnungsdatum,
                        r.faelligAm, r.waehrung, r.total, o.offenerBetrag)
                    from Rechnung r left join OffenerPosten o on o.rechnungId = r.id
                    """ + FILTER, countQuery = "select count(r) from Rechnung r " + FILTER)
    Page<RechnungZeile> sucheMitFilter(
            Rechnungsstatus status,
            UUID kundeId,
            boolean nurOffene,
            boolean nurUeberfaellige,
            LocalDate faelligBis,
            String muster,
            Collection<Rechnungsstatus> offene,
            LocalDate heute,
            Pageable seite);

    @EntityGraph(attributePaths = "positionen")
    @Query("select r from Rechnung r where r.id = :id")
    Optional<Rechnung> mitPositionen(UUID id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select r from Rechnung r where r.id = :id")
    Optional<Rechnung> mitVersionserhoehung(UUID id);

    String POSTEN = """
            select new ch.kmu.debitoren.rechnung.domain.Posten(
                r.id, r.rechnungsnummer, r.status, r.kundeId, r.empfaengerName, r.referenz, r.faelligAm,
                r.waehrung, r.total, r.mahngebuehren, o.offenerBetrag)
            from Rechnung r left join OffenerPosten o on o.rechnungId = r.id
            """;

    @Query(POSTEN + "where r.id = :id")
    Optional<Posten> postenMitId(UUID id);

    @Query(POSTEN + "where r.referenz = :referenz and r.status <> :entwurf")
    Optional<Posten> postenMitReferenz(Zahlungsreferenz referenz, Rechnungsstatus entwurf);

    @Query(POSTEN + """
                    where r.status in :offene and r.waehrung = :waehrung and o.offenerBetrag = :betrag
                    order by r.faelligAm, r.rechnungsnummer
                    """)
    List<Posten> postenMitOffenemBetrag(BigDecimal betrag, Currency waehrung, Collection<Rechnungsstatus> offene);

    @Query(
            value = POSTEN + "where r.status in :offene order by r.rechnungsnummer",
            countQuery = "select count(r) from Rechnung r where r.status in :offene")
    Page<Posten> postenZahlbar(Collection<Rechnungsstatus> offene, Pageable seite);

    @Query(POSTEN + """
                    where r.status in :offene and r.faelligAm < :stichtag and o.offenerBetrag > 0
                    order by r.faelligAm, r.rechnungsnummer
                    """)
    List<Posten> postenUeberfaellig(LocalDate stichtag, Collection<Rechnungsstatus> offene);

    /** Ohne Versionsprüfung: Das PDF entsteht nach der Freigabe, während andere die Rechnung schon verbuchen. */
    @Modifying
    @Query("update Rechnung r set r.pdfSchluessel = :schluessel where r.id = :id")
    int pdfSchluesselSetzen(UUID id, String schluessel);

    @Override
    default Rechnung speichern(Rechnung rechnung) {
        return save(rechnung);
    }

    @Override
    default Optional<Rechnung> finde(UUID id) {
        return mitPositionen(id);
    }

    @Override
    default Optional<Rechnung> zumVerbuchen(UUID id) {
        return mitVersionserhoehung(id);
    }

    @Override
    default Optional<Posten> posten(UUID id) {
        return postenMitId(id);
    }

    @Override
    default Optional<Posten> postenNachReferenz(Zahlungsreferenz referenz) {
        return postenMitReferenz(referenz, Rechnungsstatus.ENTWURF);
    }

    @Override
    default List<Posten> zahlbareMitOffenemBetrag(Geld betrag) {
        return postenMitOffenemBetrag(betrag.betrag(), betrag.waehrung(), Rechnungsstatus.OFFENE);
    }

    @Override
    default Page<Posten> zahlbare(Pageable seite) {
        return postenZahlbar(Rechnungsstatus.OFFENE, seite);
    }

    @Override
    default List<Posten> ueberfaellige(LocalDate stichtag) {
        return postenUeberfaellig(stichtag, Rechnungsstatus.OFFENE);
    }

    @Override
    default void pdfAbgelegt(UUID id, String schluessel) {
        pdfSchluesselSetzen(id, schluessel);
    }

    @Override
    default void loeschen(Rechnung rechnung) {
        delete(rechnung);
    }

    @Override
    default Page<RechnungZeile> suche(Rechnungsfilter filter, LocalDate heute, Pageable seite) {
        var muster = filter.suchtext() == null || filter.suchtext().isBlank()
                ? null
                : "%"
                        + filter.suchtext()
                                .strip()
                                .toLowerCase(Locale.ROOT)
                                .replace("%", "")
                                .replace("_", "") + "%";
        return sucheMitFilter(
                filter.status(),
                filter.kundeId(),
                filter.nurOffene(),
                filter.nurUeberfaellige(),
                filter.faelligBis(),
                muster,
                Rechnungsstatus.OFFENE,
                heute,
                seite);
    }
}
