package ch.kmu.debitoren.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Erzeugt Primärschlüssel als UUID der Version 7 (RFC 9562). Sie sind zeitlich sortiert und halten
 * B-Tree-Indizes kompakt; die Entität kennt ihre ID schon vor dem Speichern.
 */
public final class Ids {

    private static final SecureRandom ZUFALL = new SecureRandom();

    private Ids() {}

    public static UUID neu() {
        long zeitstempel = System.currentTimeMillis();
        long hoch = (zeitstempel << 16) | 0x7000L | (ZUFALL.nextLong() & 0x0FFFL);
        long tief = (ZUFALL.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(hoch, tief);
    }
}
