package ch.kmu.debitoren.shared.nummern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.ApiTestBasis;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class NummernvergabeIT extends ApiTestBasis {

    @Autowired
    Nummernvergabe nummern;

    @Autowired
    PlatformTransactionManager transaktionen;

    @Test
    void vergibtParallelLueckenlosUndOhneDoppelte() throws Exception {
        var mandant = mandantMitQrIban("Nummern AG");
        var tx = new TransactionTemplate(transaktionen);
        List<Callable<Long>> aufgaben = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            aufgaben.add(() ->
                    MandantKontext.berechnenFuer(mandant, () -> tx.execute(status -> nummern.naechste("TEST", 2026))));
        }

        var vergeben = new HashSet<Long>();
        try (var ausfuehrer = Executors.newFixedThreadPool(8)) {
            for (var ergebnis : ausfuehrer.invokeAll(aufgaben)) {
                vergeben.add(ergebnis.get());
            }
        }

        assertThat(vergeben).hasSize(200).contains(1L, 200L);
    }

    @Test
    void abgebrocheneTransaktionGibtDieNummerFrei() {
        var mandant = mandantMitQrIban("Rollback AG");
        var tx = new TransactionTemplate(transaktionen);

        MandantKontext.ausfuehrenFuer(mandant, () -> {
            tx.executeWithoutResult(status -> {
                assertThat(nummern.naechste("TEST", 2026)).isEqualTo(1);
                status.setRollbackOnly();
            });
            long wieder = tx.execute(status -> nummern.naechste("TEST", 2026));
            long anderesJahr = tx.execute(status -> nummern.naechste("TEST", 2027));
            long andereArt = tx.execute(status -> nummern.naechste("ANDERE", 2026));
            assertThat(wieder).isEqualTo(1);
            assertThat(anderesJahr).isEqualTo(1);
            assertThat(andereArt).isEqualTo(1);
        });
    }

    @Test
    void brauchtEineLaufendeTransaktion() {
        var mandant = mandantMitQrIban("Ohne Transaktion AG");
        MandantKontext.ausfuehrenFuer(
                mandant,
                () -> assertThatThrownBy(() -> nummern.naechste("TEST", 2026))
                        .isInstanceOf(IllegalTransactionStateException.class));
    }
}
