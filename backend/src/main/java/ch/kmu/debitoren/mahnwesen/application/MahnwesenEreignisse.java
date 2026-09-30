package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.stammdaten.application.MandantAngelegt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reagiert nach dem Commit auf Ereignisse, bewusst ohne eigene Transaktion: Der Mandant muss gesetzt sein, bevor
 * der aufgerufene Use Case seine Transaktion beginnt. Spring Modulith hält jedes Ereignis fest, bis es verarbeitet
 * ist.
 */
@Component
class MahnwesenEreignisse {

    private final Mahnstufen mahnstufen;
    private final Mahnungsversand versand;

    MahnwesenEreignisse(Mahnstufen mahnstufen, Mahnungsversand versand) {
        this.mahnstufen = mahnstufen;
        this.versand = versand;
    }

    @Async
    @TransactionalEventListener
    void beiMandantAngelegt(MandantAngelegt ereignis) {
        Systemkontext.fuerMandant(ereignis.mandantId(), mahnstufen::standardAnlegen);
    }

    @Async
    @TransactionalEventListener
    void beiMahnungFreigegeben(MahnungFreigegeben ereignis) {
        Systemkontext.fuerMandant(ereignis.mandantId(), () -> versand.versenden(ereignis.mahnungId()));
    }
}
