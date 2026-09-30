package ch.kmu.debitoren.shared;

import java.time.Clock;
import java.time.ZoneId;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Uhr in Schweizer Zeit, asynchrone Verarbeitung von Ereignissen zwischen Modulen und geplante Jobs, die bei
 * mehreren Instanzen nur einmal laufen.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
class GrundKonfiguration {

    static final ZoneId SCHWEIZ = ZoneId.of("Europe/Zurich");

    /** Alle fachlichen Daten (Rechnungsdatum, Fälligkeit) gelten in Schweizer Zeit. */
    @Bean
    Clock uhr() {
        return Clock.system(SCHWEIZ);
    }

    /** Sperren in der Tabelle {@code shedlock}; die Zeit liefert die Datenbank, nicht die Uhr der Instanz. */
    @Bean
    LockProvider jobSperre(DataSource datenquelle) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(datenquelle))
                .usingDbTime()
                .build());
    }
}
