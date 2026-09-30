package ch.kmu.debitoren.stammdaten.application;

import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.domain.Mandant;
import ch.kmu.debitoren.stammdaten.domain.MandantRepository;
import ch.kmu.debitoren.stammdaten.domain.StammdatenFehlercode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MandantenAbfragen {

    private final MandantRepository mandanten;

    MandantenAbfragen(MandantRepository mandanten) {
        this.mandanten = mandanten;
    }

    /** Mandanten aus dem Token des angemeldeten Benutzers, für die Mandantenwahl in der Kopfzeile. */
    @PreAuthorize("hasRole('LESER')")
    public List<Mandant> erlaubteMandanten() {
        var ids = MandantKontext.erlaubteMandanten();
        return ids.isEmpty() ? List.of() : mandanten.findeAlle(ids);
    }

    @PreAuthorize("hasRole('LESER')")
    public boolean existiert(UUID id) {
        return mandanten.existiert(id);
    }

    /** Alle Mandanten, etwa für den täglichen Mahnlauf; nur im Systemkontext. */
    @PreAuthorize("hasRole('SYSTEM')")
    public List<UUID> alleIds() {
        return mandanten.alleIds();
    }

    /** Alle Angaben des aktuellen Mandanten, etwa für die Einstellungen. */
    @PreAuthorize("hasRole('LESER')")
    public Mandant aktuellerMandant() {
        var id = MandantKontext.erforderlich();
        return mandanten
                .finde(id)
                .orElseThrow(() -> new FachlicherFehler(
                        StammdatenFehlercode.MANDANT_NICHT_GEFUNDEN, "Mandant %s nicht gefunden".formatted(id)));
    }

    /** Der Mandant, für den gerade gearbeitet wird. */
    @PreAuthorize("hasRole('LESER')")
    public MandantInfo aktueller() {
        var mandant = aktuellerMandant();
        return new MandantInfo(
                mandant.getId(),
                mandant.getName(),
                mandant.getAdresse(),
                mandant.getZahlungskonto(),
                mandant.getQrIban() == null
                        ? Set.of(mandant.getIban())
                        : Set.of(mandant.getIban(), mandant.getQrIban()),
                mandant.getReferenzart(),
                mandant.getMwstNummer(),
                mandant.getEmail(),
                mandant.isRundungFuenfRappen());
    }
}
