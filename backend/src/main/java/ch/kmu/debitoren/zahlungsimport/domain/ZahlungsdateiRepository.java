package ch.kmu.debitoren.zahlungsimport.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für importierte Dateien; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface ZahlungsdateiRepository {

    /**
     * Speichert sofort, damit ein gleichzeitiger Import derselben Datei an der Datenbank scheitert.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException wenn die Datei schon importiert ist
     */
    Zahlungsdatei speichern(Zahlungsdatei datei);

    Optional<Zahlungsdatei> finde(UUID id);

    Optional<Zahlungsdatei> findeNachHash(String sha256);

    /** Neueste zuerst, mit dem Stand der zugehörigen Zahlungen. */
    Page<ZahlungsdateiZeile> protokoll(Pageable seite);

    long anzahl();
}
