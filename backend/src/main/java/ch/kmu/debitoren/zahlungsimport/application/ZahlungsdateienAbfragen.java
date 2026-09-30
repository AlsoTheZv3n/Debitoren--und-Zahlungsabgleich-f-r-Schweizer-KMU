package ch.kmu.debitoren.zahlungsimport.application;

import ch.kmu.debitoren.shared.dokumente.Dokumentablage;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiRepository;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsimportFehlercode;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Importprotokoll und die Originale der importierten Dateien. */
@Service
@Transactional(readOnly = true)
public class ZahlungsdateienAbfragen {

    private final ZahlungsdateiRepository dateien;
    private final Dokumentablage ablage;

    ZahlungsdateienAbfragen(ZahlungsdateiRepository dateien, Dokumentablage ablage) {
        this.dateien = dateien;
        this.ablage = ablage;
    }

    @PreAuthorize("hasRole('LESER')")
    public Page<ZahlungsdateiZeile> protokoll(Pageable seite) {
        MandantKontext.erforderlich();
        return dateien.protokoll(seite);
    }

    @PreAuthorize("hasRole('LESER')")
    public long anzahl() {
        MandantKontext.erforderlich();
        return dateien.anzahl();
    }

    /** Die Datei, so wie sie hochgeladen wurde. */
    @PreAuthorize("hasRole('LESER')")
    public Original original(UUID id) {
        MandantKontext.erforderlich();
        var datei = dateien.finde(id).orElseThrow(() -> nichtGefunden(id));
        var inhalt = ablage.lesen(datei.getSpeicherort())
                .orElseThrow(() ->
                        new IllegalStateException("Original der Zahlungsdatei %s fehlt in der Ablage".formatted(id)));
        return new Original(datei.getDateiname(), inhalt);
    }

    public record Original(String dateiname, byte[] inhalt) {}

    private static FachlicherFehler nichtGefunden(UUID id) {
        return new FachlicherFehler(
                ZahlungsimportFehlercode.ZAHLUNGSDATEI_NICHT_GEFUNDEN, "Zahlungsdatei %s nicht gefunden".formatted(id));
    }
}
