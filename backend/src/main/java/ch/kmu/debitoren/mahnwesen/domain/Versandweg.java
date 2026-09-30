package ch.kmu.debitoren.mahnwesen.domain;

import org.springframework.modulith.NamedInterface;

/** Per E-Mail, wenn der Kunde eine Adresse hat; sonst druckt die Buchhaltung das Schreiben aus. */
@NamedInterface("modell")
public enum Versandweg {
    EMAIL,
    POST
}
