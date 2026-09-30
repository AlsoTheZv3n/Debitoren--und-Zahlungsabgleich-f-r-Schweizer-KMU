package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.stammdaten.application.MandantAngelegt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reagiert nach dem Commit auf Ereignisse. Bewusst ohne eigene Transaktion: Hibernate liest den Mandanten
 * beim Öffnen der Session, er muss also gesetzt sein, bevor der aufgerufene Use Case seine Transaktion
 * beginnt. Spring Modulith hält jedes Ereignis fest, bis der Empfänger es erfolgreich verarbeitet hat.
 */
@Component
class Ereignisempfaenger {

    private final MwstSaetze mwstSaetze;
    private final Rechnungsdokumente dokumente;

    Ereignisempfaenger(MwstSaetze mwstSaetze, Rechnungsdokumente dokumente) {
        this.mwstSaetze = mwstSaetze;
        this.dokumente = dokumente;
    }

    @Async
    @TransactionalEventListener
    void beiMandantAngelegt(MandantAngelegt ereignis) {
        Systemkontext.fuerMandant(ereignis.mandantId(), mwstSaetze::standardSaetzeAnlegen);
    }

    @Async
    @TransactionalEventListener
    void beiRechnungFreigegeben(RechnungFreigegeben ereignis) {
        Systemkontext.fuerMandant(ereignis.mandantId(), () -> dokumente.pdfErstellen(ereignis.rechnungId()));
    }
}
