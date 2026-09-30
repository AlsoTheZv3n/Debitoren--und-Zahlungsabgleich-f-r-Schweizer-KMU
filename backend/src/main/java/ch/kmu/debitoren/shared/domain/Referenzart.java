package ch.kmu.debitoren.shared.domain;

/** Art der Zahlungsreferenz, die sich aus dem Konto des Mandanten ergibt. */
public enum Referenzart {
    /** QR-Referenz mit 27 Ziffern; verlangt eine QR-IBAN. */
    QR,
    /** Creditor Reference nach ISO 11649 ({@code RF…}) für eine normale IBAN. */
    SCOR
}
