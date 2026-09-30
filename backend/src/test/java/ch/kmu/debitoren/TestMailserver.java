package ch.kmu.debitoren;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/** SMTP-Server im Test (GreenMail) auf einem freien Port; Tests lesen die versandten Nachrichten direkt aus. */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailserver {

    @Bean(initMethod = "start", destroyMethod = "stop")
    GreenMail mailserver() {
        return new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    }

    @Bean
    DynamicPropertyRegistrar mailserverEigenschaften(GreenMail mailserver) {
        return eigenschaften -> {
            eigenschaften.add("spring.mail.host", () -> "127.0.0.1");
            eigenschaften.add("spring.mail.port", () -> mailserver.getSmtp().getPort());
        };
    }
}
