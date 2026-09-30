package ch.kmu.debitoren.abgleich.application;

import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungenImportiert;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gleicht neu importierte Zahlungen nach dem Commit des Imports ab, jede in ihrer eigenen Transaktion. Scheitert
 * eine Zahlung, laufen die übrigen weiter; das Ereignis bleibt dann offen und Spring Modulith stellt es nach
 * einem Neustart erneut zu. Bereits abgeglichene Zahlungen überspringt der Abgleich.
 *
 * <p>Bewusst ohne eigene Transaktion: Der Mandant muss gesetzt sein, bevor Hibernate die Session öffnet.
 */
@Component
class AbgleichNachImport {

    private static final Logger LOG = LoggerFactory.getLogger(AbgleichNachImport.class);

    private final Zahlungsabgleich abgleich;

    AbgleichNachImport(Zahlungsabgleich abgleich) {
        this.abgleich = abgleich;
    }

    @Async
    @TransactionalEventListener
    void beiZahlungenImportiert(ZahlungenImportiert ereignis) {
        var gescheitert = new ArrayList<UUID>();
        for (var zahlungId : ereignis.zahlungIds()) {
            try {
                Systemkontext.fuerMandant(ereignis.mandantId(), () -> abgleich.abgleichen(zahlungId));
            } catch (RuntimeException e) {
                LOG.warn("Abgleich der Zahlung {} gescheitert", zahlungId, e);
                gescheitert.add(zahlungId);
            }
        }
        if (!gescheitert.isEmpty()) {
            throw new IllegalStateException("Abgleich unvollständig, gescheitert: " + gescheitert);
        }
    }
}
