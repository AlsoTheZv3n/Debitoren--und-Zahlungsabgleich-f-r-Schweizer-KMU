package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.shared.security.Systemkontext;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Startet den Mahnlauf jeden Morgen für alle Mandanten. ShedLock sorgt dafür, dass er bei mehreren Instanzen nur
 * einmal läuft; scheitert er bei einem Mandanten, laufen die übrigen weiter.
 */
@Component
class MahnlaufPlanung {

    private static final Logger LOG = LoggerFactory.getLogger(MahnlaufPlanung.class);

    private final MandantenAbfragen mandanten;
    private final Mahnlaeufe laeufe;

    MahnlaufPlanung(MandantenAbfragen mandanten, Mahnlaeufe laeufe) {
        this.mandanten = mandanten;
        this.laeufe = laeufe;
    }

    @Scheduled(cron = "${debitoren.mahnlauf.zeitplan:0 0 6 * * *}", zone = "Europe/Zurich")
    @SchedulerLock(name = "mahnlauf", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    void taeglich() {
        for (var mandant : Systemkontext.ausfuehren(mandanten::alleIds)) {
            try {
                Systemkontext.fuerMandant(mandant, () -> {
                    var lauf = laeufe.ausfuehren(laeufe.heute());
                    LOG.info(
                            "Mahnlauf für Mandant {}: {} überfällig, {} neue Vorschläge, {} verworfen",
                            mandant,
                            lauf.getGeprueft(),
                            lauf.getVorschlaege(),
                            lauf.getVerworfen());
                });
            } catch (RuntimeException e) {
                LOG.error("Mahnlauf für Mandant {} gescheitert", mandant, e);
            }
        }
    }
}
