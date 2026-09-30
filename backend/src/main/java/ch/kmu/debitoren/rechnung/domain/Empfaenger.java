package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Pruefe;

/** Name und Adresse des Kunden, wie sie auf der Rechnung stehen. */
public record Empfaenger(String name, Adresse adresse) {

    public Empfaenger {
        name = Pruefe.pflichttext(name, "Name des Empfängers", 70);
        Pruefe.vorhanden(adresse, "Adresse des Empfängers");
    }
}
