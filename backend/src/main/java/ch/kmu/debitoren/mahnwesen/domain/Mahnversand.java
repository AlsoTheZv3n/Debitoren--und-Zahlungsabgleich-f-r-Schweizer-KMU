package ch.kmu.debitoren.mahnwesen.domain;

/** Port für den Versand per E-Mail; lokal fängt Mailpit alles ab, produktiv geht es an den SMTP-Dienst. */
public interface Mahnversand {

    /**
     * Übergibt die Mahnung dem Mailserver.
     *
     * @throws VersandFehlgeschlagen wenn der Server sie nicht annimmt
     */
    void senden(Mahnmail mail);

    /**
     * @param antwortAn Adresse des Mandanten für Antworten; null, wenn er keine hinterlegt hat
     * @param absenderName Name des Mandanten als Anzeigename des Absenders
     */
    record Mahnmail(
            String an,
            String antwortAn,
            String absenderName,
            String betreff,
            String text,
            String dateiname,
            byte[] pdf) {}

    /** Der Mailserver hat die Nachricht nicht angenommen. */
    class VersandFehlgeschlagen extends RuntimeException {

        public VersandFehlgeschlagen(String meldung, Throwable ursache) {
            super(meldung, ursache);
        }
    }
}
