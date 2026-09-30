package ch.kmu.debitoren.shared.dokumente;

import java.util.Optional;

/**
 * Port für unveränderliche Dokumente wie Rechnungs-PDFs und camt-Originale, die 10 Jahre aufbewahrt
 * werden (OR Art. 958f). Lokal liegt die Ablage im Dateisystem, produktiv in S3 mit Versionierung.
 */
public interface Dokumentablage {

    /**
     * Legt ein Dokument ab. Ein Schlüssel wird nur einmal beschrieben.
     *
     * @throws DokumentBereitsAbgelegt wenn unter dem Schlüssel schon ein Dokument liegt
     */
    void ablegen(String schluessel, byte[] inhalt);

    Optional<byte[]> lesen(String schluessel);
}
