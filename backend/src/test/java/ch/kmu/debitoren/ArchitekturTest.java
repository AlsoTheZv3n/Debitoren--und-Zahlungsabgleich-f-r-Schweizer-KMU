package ch.kmu.debitoren;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/** Schichten innerhalb der Module: api → application → domain, infrastructure implementiert Ports. */
@AnalyzeClasses(packages = "ch.kmu.debitoren", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitekturTest {

    private static final String API = "ch.kmu.debitoren..api..";
    private static final String APPLICATION = "ch.kmu.debitoren..application..";
    private static final String DOMAIN = "ch.kmu.debitoren..domain..";
    private static final String INFRASTRUCTURE = "ch.kmu.debitoren..infrastructure..";

    @ArchTest
    static final ArchRule domaeneKenntWederApiNochInfrastrukturNochWeb = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(API, APPLICATION, INFRASTRUCTURE, "org.springframework.web..", "jakarta.servlet..")
            .because("die Domäne darf weder api noch infrastructure noch Spring Web kennen");

    @ArchTest
    static final ArchRule useCasesKennenWederApiNochInfrastruktur = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(API, INFRASTRUCTURE, "org.springframework.web..")
            .because("Use Cases hängen nur von Ports ab");

    @ArchTest
    static final ArchRule infrastrukturKenntDieApiNicht = noClasses()
            .that()
            .resideInAPackage(INFRASTRUCTURE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(API);

    @ArchTest
    static final ArchRule controllerLiegenInApi = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .and()
            .resideOutsideOfPackage("ch.kmu.debitoren.shared..")
            .should()
            .resideInAPackage(API);

    @ArchTest
    static final ArchRule betraegeNieAlsGleitkommazahl = noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(DOMAIN)
            .should()
            .haveRawType(double.class)
            .orShould()
            .haveRawType(Double.class)
            .orShould()
            .haveRawType(float.class)
            .orShould()
            .haveRawType(Float.class)
            .because("Beträge sind BigDecimal oder Geld, nie double")
            .allowEmptyShould(true);
}
