package ch.kmu.debitoren.stammdaten.application;

import java.util.UUID;

/** Ein neuer Mandant ist angelegt; andere Module richten ihre Grundkonfiguration ein, etwa die MWST-Sätze. */
public record MandantAngelegt(UUID mandantId) {}
