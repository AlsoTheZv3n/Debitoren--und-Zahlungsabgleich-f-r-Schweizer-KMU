package ch.kmu.debitoren.rechnung.infrastructure;

import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungsdokument;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungssteller;
import ch.kmu.debitoren.shared.domain.Adresse;
import java.util.ArrayList;
import java.util.List;

/** Fertig formatierte Texte für die Vorlage {@code templates/dokumente/rechnung.html}. */
record RechnungAnsicht(
        String titel,
        String absenderName,
        List<String> absenderZeilen,
        List<String> empfaengerZeilen,
        List<Angabe> angaben,
        String bemerkung,
        List<Zeile> positionen,
        List<Angabe> summen,
        String totalBezeichnung,
        String total,
        String zahlungshinweis) {

    record Angabe(String bezeichnung, String wert) {}

    record Zeile(
            String position,
            String beschreibung,
            String menge,
            String einheit,
            String einzelpreis,
            String mwst,
            String betrag) {}

    static RechnungAnsicht aus(Rechnungsdokument dokument) {
        var steller = dokument.steller();
        var rechnung = dokument.rechnung();
        var waehrung = rechnung.getWaehrung().getCurrencyCode();
        var summen = rechnung.summen();

        var absender = absenderzeilen(steller);

        var empfaenger = new ArrayList<String>();
        empfaenger.add(rechnung.getEmpfaenger().name());
        empfaenger.addAll(adresszeilen(rechnung.getEmpfaenger().adresse()));

        var angaben = List.of(
                new Angabe("Rechnungsdatum", Formate.datum(rechnung.getRechnungsdatum())),
                new Angabe("Kundennummer", dokument.kundennummer()),
                new Angabe("Zahlbar bis", Formate.datum(rechnung.getFaelligAm())),
                new Angabe("Referenz", rechnung.getReferenz().formatiert()));

        var positionen = rechnung.getPositionen().stream()
                .map(position -> new Zeile(
                        String.valueOf(position.getPosition()),
                        position.getBeschreibung(),
                        Formate.menge(position.getMenge()),
                        position.getEinheit() == null ? "" : position.getEinheit(),
                        Formate.betrag(position.getEinzelpreis()),
                        Formate.prozent(position.getMwstProzent()),
                        Formate.betrag(position.getBetrag())))
                .toList();

        var summenzeilen = new ArrayList<Angabe>();
        summenzeilen.add(
                new Angabe("Total ohne MWST", Formate.betrag(summen.netto().betrag())));
        for (var anteil : summen.mwst()) {
            summenzeilen.add(new Angabe(
                    "MWST %s auf %s"
                            .formatted(
                                    Formate.prozent(anteil.prozent()),
                                    Formate.betrag(anteil.basis().betrag())),
                    Formate.betrag(anteil.betrag().betrag())));
        }
        if (!summen.rundung().istNull()) {
            summenzeilen.add(
                    new Angabe("Rundung", Formate.betrag(summen.rundung().betrag())));
        }

        return new RechnungAnsicht(
                "Rechnung " + rechnung.getRechnungsnummer(),
                steller.name(),
                absender,
                empfaenger,
                angaben,
                rechnung.getBemerkung(),
                positionen,
                summenzeilen,
                "Total " + waehrung,
                Formate.betrag(summen.total().betrag()),
                "Bitte überweisen Sie den Betrag bis am %s mit dem QR-Zahlteil unten."
                        .formatted(Formate.datum(rechnung.getFaelligAm())));
    }

    /** Adresse, MWST-Nummer und E-Mail des Absenders, wie sie unter seinem Namen stehen. */
    static List<String> absenderzeilen(Rechnungssteller steller) {
        var zeilen = new ArrayList<>(adresszeilen(steller.adresse()));
        if (steller.mwstNummer() != null) {
            zeilen.add("MWST-Nr. " + steller.mwstNummer());
        }
        if (steller.email() != null) {
            zeilen.add(steller.email());
        }
        return zeilen;
    }

    static List<String> adresszeilen(Adresse adresse) {
        var zeilen = new ArrayList<String>();
        if (adresse.strasse() != null) {
            zeilen.add(
                    adresse.hausnummer() == null ? adresse.strasse() : adresse.strasse() + " " + adresse.hausnummer());
        }
        zeilen.add(("CH".equals(adresse.land()) ? "" : adresse.land() + "-") + adresse.plz() + " " + adresse.ort());
        return zeilen;
    }
}
