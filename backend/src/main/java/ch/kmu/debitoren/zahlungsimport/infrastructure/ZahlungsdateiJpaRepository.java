package ch.kmu.debitoren.zahlungsimport.infrastructure;

import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsdatei;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiRepository;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Adapter: Spring Data implementiert den Port {@link ZahlungsdateiRepository}; Hibernate filtert nach Mandant. */
interface ZahlungsdateiJpaRepository extends JpaRepository<Zahlungsdatei, UUID>, ZahlungsdateiRepository {

    Optional<Zahlungsdatei> findBySha256(String sha256);

    @Query(value = """
                    select new ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile(
                        d.id, d.dateiname, d.nachrichtId, d.erstelltVonBankAm, d.importiertAm, d.importiertVon,
                        d.anzahlBuchungen, d.anzahlNeu, d.anzahlUebersprungen, d.anzahlIgnoriert,
                        (select count(z) from Zahlung z where z.zahlungsdateiId = d.id and z.status = :offen),
                        (select count(z) from Zahlung z where z.zahlungsdateiId = d.id and z.status = :zugeordnet),
                        (select count(z) from Zahlung z where z.zahlungsdateiId = d.id and z.status = :klaerung))
                    from Zahlungsdatei d
                    order by d.importiertAm desc
                    """, countQuery = "select count(d) from Zahlungsdatei d")
    Page<ZahlungsdateiZeile> protokollMitStatus(
            Zahlungsstatus offen, Zahlungsstatus zugeordnet, Zahlungsstatus klaerung, Pageable seite);

    @Override
    default Zahlungsdatei speichern(Zahlungsdatei datei) {
        return saveAndFlush(datei);
    }

    @Override
    default Optional<Zahlungsdatei> finde(UUID id) {
        return findById(id);
    }

    @Override
    default Optional<Zahlungsdatei> findeNachHash(String sha256) {
        return findBySha256(sha256);
    }

    @Override
    default Page<ZahlungsdateiZeile> protokoll(Pageable seite) {
        return protokollMitStatus(Zahlungsstatus.OFFEN, Zahlungsstatus.ZUGEORDNET, Zahlungsstatus.KLAERUNG, seite);
    }

    @Override
    default long anzahl() {
        return count();
    }
}
