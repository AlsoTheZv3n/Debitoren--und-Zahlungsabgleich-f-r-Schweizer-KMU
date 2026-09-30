package ch.kmu.debitoren.testdaten;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Umfang der Testdaten je Mandant. Die Vorgabe entspricht der Projektplanung; Tests und der E2E-Stack
 * nehmen weniger, damit sie schnell starten.
 */
@ConfigurationProperties("debitoren.testdaten")
record TestdatenEigenschaften(
        @DefaultValue("200") int kundenJeMandant,
        @DefaultValue("1500") int rechnungenJeMandant,
        @DefaultValue("20260929") long seed) {}
