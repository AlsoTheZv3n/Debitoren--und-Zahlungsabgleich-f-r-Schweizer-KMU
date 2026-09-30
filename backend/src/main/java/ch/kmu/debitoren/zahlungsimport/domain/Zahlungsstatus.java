package ch.kmu.debitoren.zahlungsimport.domain;

import org.springframework.modulith.NamedInterface;

/** Stand einer Gutschrift im Abgleich. */
@NamedInterface("modell")
public enum Zahlungsstatus {
    /** Importiert, der Abgleich hat sie noch nicht bearbeitet. */
    OFFEN,
    /** Vollständig einer oder mehreren Rechnungen zugeordnet. */
    ZUGEORDNET,
    /** Ganz oder teilweise ungeklärt; der Grund steht dabei. */
    KLAERUNG
}
