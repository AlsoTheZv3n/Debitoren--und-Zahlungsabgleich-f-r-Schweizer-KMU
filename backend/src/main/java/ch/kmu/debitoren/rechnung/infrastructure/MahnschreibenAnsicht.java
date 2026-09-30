package ch.kmu.debitoren.rechnung.infrastructure;

import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Mahndokument;
import ch.kmu.debitoren.rechnung.infrastructure.RechnungAnsicht.Angabe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fertig formatierte Texte für die Vorlage {@code templates/dokumente/mahnung.html}. */
record MahnschreibenAnsicht(
        String titel,
        String absenderName,
        List<String> absenderZeilen,
        List<String> empfaengerZeilen,
        String ortUndDatum,
        List<Angabe> angaben,
        List<String> absaetze,
        List<Angabe> betraege,
        String totalBezeichnung,
        String total,
        String zahlungshinweis) {

    static MahnschreibenAnsicht aus(Mahndokument dokument) {
        var steller = dokument.steller();
        var rechnung = dokument.rechnung();
        var angaben = dokument.angaben();
        var waehrung = rechnung.getWaehrung().getCurrencyCode();

        var empfaenger = new ArrayList<String>();
        empfaenger.add(dokument.empfaenger().name());
        empfaenger.addAll(RechnungAnsicht.adresszeilen(dokument.empfaenger().adresse()));

        var kopf = List.of(
                new Angabe("Rechnung", rechnung.getRechnungsnummer()),
                new Angabe("Rechnungsdatum", Formate.datum(rechnung.getRechnungsdatum())),
                new Angabe("Fällig seit", Formate.datum(rechnung.getFaelligAm())),
                new Angabe("Kundennummer", dokument.kundennummer()),
                new Angabe("Referenz", rechnung.getReferenz().formatiert()));

        var betraege = new ArrayList<Angabe>();
        betraege.add(new Angabe(
                "Rechnungsbetrag", Formate.betrag(angaben.rechnungsbetrag().betrag())));
        if (angaben.bezahlt().istPositiv()) {
            betraege.add(new Angabe(
                    "Bereits bezahlt",
                    Formate.betrag(angaben.bezahlt().negiert().betrag())));
        }
        if (angaben.mahngebuehren().istPositiv()) {
            betraege.add(new Angabe(
                    "Mahngebühren", Formate.betrag(angaben.mahngebuehren().betrag())));
        }

        return new MahnschreibenAnsicht(
                angaben.titel(),
                steller.name(),
                RechnungAnsicht.absenderzeilen(steller),
                empfaenger,
                steller.adresse().ort() + ", " + Formate.datum(angaben.mahndatum()),
                kopf,
                absaetze(angaben.text()),
                betraege,
                "Offener Betrag " + waehrung,
                Formate.betrag(angaben.offen().betrag()),
                ("Bitte überweisen Sie den offenen Betrag bis am %s mit dem QR-Zahlteil unten. Haben Sie in der"
                                + " Zwischenzeit bezahlt, betrachten Sie dieses Schreiben als gegenstandslos.")
                        .formatted(Formate.datum(angaben.fristBis())));
    }

    /** Eine Leerzeile trennt Absätze; Zeilenumbrüche innerhalb eines Absatzes gelten als Leerzeichen. */
    static List<String> absaetze(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.strip().split("\\R\\s*\\R"))
                .map(absatz -> absatz.strip().replaceAll("\\s*\\R\\s*", " "))
                .filter(absatz -> !absatz.isEmpty())
                .toList();
    }
}
