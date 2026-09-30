package ch.kmu.debitoren.shared.domain;

import jakarta.persistence.Embeddable;

/**
 * Strukturierte Adresse mit den Feldlängen des QR-Zahlteils. Der Name gehört zur Person oder Firma, nicht
 * zur Adresse.
 */
@Embeddable
public record Adresse(String strasse, String hausnummer, String plz, String ort, String land) {

    public Adresse {
        strasse = Pruefe.optionalerText(strasse, "Strasse", 70);
        hausnummer = Pruefe.optionalerText(hausnummer, "Hausnummer", 16);
        plz = Pruefe.pflichttext(plz, "PLZ", 16);
        ort = Pruefe.pflichttext(ort, "Ort", 35);
        land = Pruefe.pflichttext(land, "Land", 2);
        if (!land.matches("[A-Z]{2}")) {
            throw Pruefe.ungueltig("Land muss ein ISO-Code aus zwei Grossbuchstaben sein, etwa CH");
        }
    }

    public static Adresse schweiz(String strasse, String hausnummer, String plz, String ort) {
        return new Adresse(strasse, hausnummer, plz, ort, "CH");
    }
}
