package ch.kmu.debitoren.shared;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Uhr in Schweizer Zeit und asynchrone Verarbeitung von Ereignissen zwischen Modulen. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class GrundKonfiguration {

    static final ZoneId SCHWEIZ = ZoneId.of("Europe/Zurich");

    /** Alle fachlichen Daten (Rechnungsdatum, Fälligkeit) gelten in Schweizer Zeit. */
    @Bean
    Clock uhr() {
        return Clock.system(SCHWEIZ);
    }
}
