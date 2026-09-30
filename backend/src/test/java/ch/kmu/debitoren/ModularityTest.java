package ch.kmu.debitoren;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Modulgrenzen nach Spring Modulith: keine Zyklen, kein Zugriff auf interne Pakete anderer Module. */
class ModularityTest {

    private static final ApplicationModules MODULE = ApplicationModules.of(DebitorenApplication.class);

    @Test
    void modulgrenzenWerdenEingehalten() {
        MODULE.verify();
    }

    @Test
    void alleFachmoduleSindVorhanden() {
        assertThat(MODULE.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
                .contains(
                        "stammdaten",
                        "rechnung",
                        "zahlungsimport",
                        "abgleich",
                        "mahnwesen",
                        "reporting",
                        "shared",
                        "testdaten");
    }

    /** Schreibt Diagramme und Modulbeschreibungen nach target/spring-modulith-docs. */
    @Test
    void dokumentationSchreiben() {
        new Documenter(MODULE)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
