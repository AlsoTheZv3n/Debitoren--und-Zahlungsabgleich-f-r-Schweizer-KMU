package ch.kmu.debitoren.mahnwesen.application;

import java.util.UUID;

/** Eine Mahnung ist freigegeben; danach entsteht ihr Schreiben, und es geht per E-Mail hinaus. */
public record MahnungFreigegeben(UUID mandantId, UUID mahnungId) {}
