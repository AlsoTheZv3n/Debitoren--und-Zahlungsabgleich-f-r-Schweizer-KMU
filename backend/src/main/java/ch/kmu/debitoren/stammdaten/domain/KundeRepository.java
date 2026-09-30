package ch.kmu.debitoren.stammdaten.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Port für Kunden; alle Zugriffe gelten nur für den Mandanten im {@code MandantKontext}. */
public interface KundeRepository {

    Kunde speichern(Kunde kunde);

    Optional<Kunde> finde(UUID id);

    Optional<Kunde> findeNachKundennummer(String kundennummer);

    /** Sucht in Name, Kundennummer und Ort; ohne Suchtext alle Kunden. */
    Page<Kunde> suche(String suchtext, Pageable seite);

    long anzahl();
}
