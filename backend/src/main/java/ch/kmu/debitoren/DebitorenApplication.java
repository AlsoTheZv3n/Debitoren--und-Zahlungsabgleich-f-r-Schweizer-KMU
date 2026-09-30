package ch.kmu.debitoren;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(systemName = "Debitoren", sharedModules = "shared")
public class DebitorenApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebitorenApplication.class, args);
    }
}
