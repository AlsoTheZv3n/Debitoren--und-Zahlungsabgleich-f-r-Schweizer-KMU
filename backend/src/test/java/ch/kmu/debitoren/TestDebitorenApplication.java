package ch.kmu.debitoren;

import org.springframework.boot.SpringApplication;

/**
 * Startet die Anwendung mit PostgreSQL aus Testcontainers, ohne Docker Compose. Dient dem Maven-Profil
 * {@code openapi} und lokal {@code ./mvnw spring-boot:test-run}.
 */
public class TestDebitorenApplication {

    public static void main(String[] args) {
        SpringApplication.from(DebitorenApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
