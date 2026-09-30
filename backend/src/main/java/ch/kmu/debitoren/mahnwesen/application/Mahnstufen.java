package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.Mahnstufe;
import ch.kmu.debitoren.mahnwesen.domain.MahnstufeRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnstufendaten;
import ch.kmu.debitoren.mahnwesen.domain.StandardMahnstufen;
import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Konfiguration der Mahnstufen je Mandant. */
@Service
public class Mahnstufen {

    private final MahnstufeRepository stufen;

    Mahnstufen(MahnstufeRepository stufen) {
        this.stufen = stufen;
    }

    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public List<Mahnstufe> auflisten() {
        MandantKontext.erforderlich();
        return stufen.alle();
    }

    /**
     * Legt alle Stufen auf einmal fest. Die Stufe an Position n ersetzt die bisherige Stufe n; überzählige werden
     * gelöscht. Schon versandte Mahnungen behalten Bezeichnung, Frist und Gebühr ihrer Stufe.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<Mahnstufe> festlegen(List<Festlegung> neu) {
        MandantKontext.erforderlich();
        Mahnstufendaten.folgePruefen(neu.stream().map(Festlegung::daten).toList());
        var bisher = stufen.alle();
        var ergebnis = new ArrayList<Mahnstufe>();
        for (int i = 0; i < neu.size(); i++) {
            var festlegung = neu.get(i);
            if (i < bisher.size()) {
                var stufe = bisher.get(i);
                if (!Objects.equals(stufe.getVersion(), festlegung.version())) {
                    throw geaendert();
                }
                stufe.aendern(festlegung.daten());
                ergebnis.add(stufe);
            } else {
                if (festlegung.version() != null) {
                    throw geaendert();
                }
                ergebnis.add(stufen.speichern(new Mahnstufe(i + 1, festlegung.daten())));
            }
        }
        bisher.subList(Math.min(neu.size(), bisher.size()), bisher.size()).forEach(stufen::loeschen);
        return ergebnis;
    }

    /** Richtet die Standardstufen ein, sofern der Mandant noch keine hat. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void standardAnlegen() {
        MandantKontext.erforderlich();
        if (!stufen.vorhanden()) {
            StandardMahnstufen.neueStufen().forEach(stufen::speichern);
        }
    }

    private static FachlicherFehler geaendert() {
        return new FachlicherFehler(
                AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                "Die Mahnstufen wurden inzwischen geändert. Bitte neu laden.");
    }

    /**
     * Eine Stufe, wie sie gespeichert werden soll.
     *
     * @param version Version der bisherigen Stufe an dieser Position; null für eine neue Stufe
     */
    public record Festlegung(Long version, Mahnstufendaten daten) {}
}
