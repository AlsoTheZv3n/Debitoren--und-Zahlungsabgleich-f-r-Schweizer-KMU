package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Empfaenger;
import ch.kmu.debitoren.rechnung.domain.MwstSatzRepository;
import ch.kmu.debitoren.rechnung.domain.Positionsdaten;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.Rechnungskopf;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.stammdaten.application.KundeInfo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Macht aus den Eingaben eines Entwurfs Kopf, Empfänger und Positionen mit aufgelösten MWST-Sätzen. */
@Component
class EntwurfAufloesung {

    private final MwstSatzRepository saetze;

    EntwurfAufloesung(MwstSatzRepository saetze) {
        this.saetze = saetze;
    }

    Rechnungskopf kopf(Entwurf entwurf, KundeInfo kunde) {
        var frist = entwurf.zahlungsfristTage() != null
                ? entwurf.zahlungsfristTage()
                : kunde != null ? kunde.zahlungsfristTage() : 30;
        return new Rechnungskopf(entwurf.rechnungsdatum(), frist, entwurf.waehrung(), entwurf.bemerkung());
    }

    static Empfaenger empfaenger(KundeInfo kunde) {
        return new Empfaenger(kunde.name(), kunde.adresse());
    }

    List<Positionsdaten> positionen(Entwurf entwurf) {
        var datum = Pruefe.vorhanden(entwurf.rechnungsdatum(), "Rechnungsdatum");
        return entwurf.positionen().stream()
                .map(position -> new Positionsdaten(
                        position.beschreibung(),
                        position.menge(),
                        position.einheit(),
                        position.einzelpreis(),
                        saetze.gueltigAm(Pruefe.vorhanden(position.mwstCode(), "MWST-Code"), datum)
                                .orElseThrow(() -> new FachlicherFehler(
                                        RechnungFehlercode.MWST_SATZ_UNBEKANNT,
                                        "Für %s gibt es am %s keinen MWST-Satz."
                                                .formatted(position.mwstCode(), datum)))))
                .toList();
    }
}
