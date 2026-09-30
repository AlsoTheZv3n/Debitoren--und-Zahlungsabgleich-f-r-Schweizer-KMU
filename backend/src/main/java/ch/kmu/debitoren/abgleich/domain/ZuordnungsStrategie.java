package ch.kmu.debitoren.abgleich.domain;

import java.util.Optional;

/**
 * Eine Regel, die zu einer Zahlung eine Rechnung vorschlägt. Die Strategien laufen in der Reihenfolge ihrer
 * {@code @Order}; eine neue Regel ist eine neue Klasse, der Abgleich selbst bleibt unverändert.
 *
 * <p>Vertrag: Kein Treffer ist ein leeres Optional, nie eine Ausnahme. Nur Vorschläge mit {@link
 * ZuordnungsVorschlag#automatisch()} verbucht der Abgleich ohne Rückfrage.
 */
public interface ZuordnungsStrategie {

    /** Kurzer, stabiler Name, der bei jeder Zuordnung als Regel gespeichert wird. */
    String regel();

    Optional<ZuordnungsVorschlag> vorschlagen(Zahlungsfall fall);
}
