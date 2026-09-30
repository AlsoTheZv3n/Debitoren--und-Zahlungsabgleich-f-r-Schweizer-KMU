package ch.kmu.debitoren.shared.dokumente;

/** Unter dem Schlüssel liegt schon ein Dokument; abgelegte Dokumente werden nie überschrieben. */
public class DokumentBereitsAbgelegt extends RuntimeException {

    public DokumentBereitsAbgelegt(String schluessel) {
        super("Dokument bereits abgelegt: " + schluessel);
    }
}
