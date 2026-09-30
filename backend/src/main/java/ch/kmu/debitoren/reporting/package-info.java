/**
 * Übersicht, Altersstruktur und Zahlungseingänge für das Dashboard, dazu das Audit-Log. Liest nur: direkt per SQL
 * aus den Sichten und Tabellen der Fachmodule, ohne deren Java-Klassen zu kennen.
 */
@ApplicationModule(displayName = "Reporting")
package ch.kmu.debitoren.reporting;

import org.springframework.modulith.ApplicationModule;
