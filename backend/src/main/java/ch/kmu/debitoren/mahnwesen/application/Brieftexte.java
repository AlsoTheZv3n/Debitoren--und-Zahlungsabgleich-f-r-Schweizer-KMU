package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.Mahnstufe;
import ch.kmu.debitoren.mahnwesen.domain.MahnstufeRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahntext;
import ch.kmu.debitoren.mahnwesen.domain.Mahnung;
import ch.kmu.debitoren.shared.domain.Geld;
import org.springframework.stereotype.Component;

/** Setzt den Text der Mahnstufe für eine Mahnung ein; fehlt die Stufe inzwischen, gilt ein Ersatztext. */
@Component
class Brieftexte {

    private final MahnstufeRepository stufen;

    Brieftexte(MahnstufeRepository stufen) {
        this.stufen = stufen;
    }

    String fuer(Mahnung mahnung, Geld zuZahlen) {
        var vorlage = stufen.alle().stream()
                .filter(stufe -> stufe.getStufe() == mahnung.getStufe())
                .map(Mahnstufe::getTextvorlage)
                .findFirst()
                .orElse(Mahntext.ERSATZ);
        return Mahntext.einsetzen(
                vorlage,
                mahnung.getRechnungsnummer(),
                mahnung.getFaelligAm(),
                zuZahlen,
                mahnung.getFristBis(),
                mahnung.getGebuehr());
    }
}
