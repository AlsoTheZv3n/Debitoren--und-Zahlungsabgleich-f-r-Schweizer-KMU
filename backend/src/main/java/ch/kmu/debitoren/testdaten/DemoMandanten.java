package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.stammdaten.application.NeuerMandant;
import java.util.List;
import java.util.UUID;

/**
 * Die beiden Demo-Mandanten. Ihre IDs stehen auch im Keycloak-Realm-Import
 * ({@code deploy/compose/keycloak/debitoren-realm.json}) als Attribut {@code mandant_id} der Benutzer.
 * Die Konten sind die Beispielkonten aus den Implementation Guidelines von SIX; Firmen und MWST-Nummern
 * sind erfunden.
 */
public final class DemoMandanten {

    /** Mandant mit QR-IBAN, stellt Rechnungen mit QR-Referenz. */
    public static final UUID BERNER_WERKSTATT = UUID.fromString("019a0000-0000-7000-8000-000000000001");

    /** Mandant mit normaler IBAN, stellt Rechnungen mit SCOR-Referenz. */
    public static final UUID SEELAND_GARTENBAU = UUID.fromString("019a0000-0000-7000-8000-000000000002");

    public static final List<NeuerMandant> ALLE = List.of(
            new NeuerMandant(
                    BERNER_WERKSTATT,
                    "Berner Werkstatt AG",
                    Adresse.schweiz("Marktgasse", "28", "3011", "Bern"),
                    new Iban("CH5800791123000889012"),
                    new Iban("CH4431999123000889012"),
                    "CHE-100.200.300 MWST",
                    "rechnungen@berner-werkstatt.example"),
            new NeuerMandant(
                    SEELAND_GARTENBAU,
                    "Seeland Gartenbau GmbH",
                    Adresse.schweiz("Hauptstrasse", "5", "2560", "Nidau"),
                    new Iban("CH9300762011623852957"),
                    null,
                    "CHE-400.500.600 MWST",
                    "buchhaltung@seeland-gartenbau.example"));

    private DemoMandanten() {}
}
