package ch.kmu.debitoren.zahlungsimport.domain;

import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/**
 * Eine importierte Bankdatei. Sie wird nur einmal geschrieben; das Original liegt unter {@code speicherort}
 * in der Dokumentablage.
 */
@Entity
@Immutable
@Table(name = "zahlungsdatei")
public class Zahlungsdatei {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Column(nullable = false)
    private String sha256;

    @Column(nullable = false)
    private String dateiname;

    @Column(nullable = false)
    private String speicherort;

    @Column(nullable = false)
    private String nachrichtId;

    @Column(nullable = false)
    private Instant erstelltVonBankAm;

    @Column(nullable = false)
    private Instant importiertAm;

    @Column(nullable = false)
    private String importiertVon;

    private int anzahlBuchungen;

    private int anzahlNeu;

    private int anzahlUebersprungen;

    private int anzahlIgnoriert;

    protected Zahlungsdatei() {}

    /**
     * @param neu Gutschriften, die als Zahlung gespeichert werden
     * @param uebersprungen Gutschriften, deren Bankreferenz schon bekannt ist
     */
    public Zahlungsdatei(
            UUID id,
            String sha256,
            String dateiname,
            String speicherort,
            Zahlungsdateiinhalt inhalt,
            int neu,
            int uebersprungen,
            Instant importiertAm,
            String importiertVon) {
        this.id = Pruefe.vorhanden(id, "ID");
        this.sha256 = Pruefe.pflichttext(sha256, "SHA-256", 64);
        this.dateiname = dateiname(dateiname);
        this.speicherort = Pruefe.pflichttext(speicherort, "Speicherort", 200);
        Pruefe.vorhanden(inhalt, "Inhalt");
        this.nachrichtId = Pruefe.pflichttext(inhalt.nachrichtId(), "Nachrichten-ID", 35);
        this.erstelltVonBankAm = Pruefe.vorhanden(inhalt.erstelltAm(), "Erstellungszeitpunkt");
        if (neu < 0
                || uebersprungen < 0
                || neu + uebersprungen != inhalt.gutschriften().size()) {
            throw Pruefe.ungueltig("Neue und übersprungene Buchungen ergeben nicht die Zahl der Gutschriften");
        }
        this.anzahlBuchungen = inhalt.gutschriften().size();
        this.anzahlNeu = neu;
        this.anzahlUebersprungen = uebersprungen;
        this.anzahlIgnoriert = inhalt.ignoriert();
        this.importiertAm = Pruefe.vorhanden(importiertAm, "Importzeitpunkt");
        this.importiertVon = Pruefe.pflichttext(importiertVon, "Benutzer", 100);
    }

    /** Nur der Name, nie ein Pfad des hochladenden Rechners. */
    static String dateiname(String name) {
        var ohnePfad = name == null ? "" : name.replace('\\', '/');
        ohnePfad = ohnePfad.substring(ohnePfad.lastIndexOf('/') + 1).strip();
        if (ohnePfad.isEmpty()) {
            return "zahlungsdatei.xml";
        }
        return ohnePfad.length() <= 255 ? ohnePfad : ohnePfad.substring(ohnePfad.length() - 255);
    }

    public UUID getId() {
        return id;
    }

    public String getSha256() {
        return sha256;
    }

    public String getDateiname() {
        return dateiname;
    }

    public String getSpeicherort() {
        return speicherort;
    }

    public String getNachrichtId() {
        return nachrichtId;
    }

    public Instant getErstelltVonBankAm() {
        return erstelltVonBankAm;
    }

    public Instant getImportiertAm() {
        return importiertAm;
    }

    public String getImportiertVon() {
        return importiertVon;
    }

    public int getAnzahlBuchungen() {
        return anzahlBuchungen;
    }

    public int getAnzahlNeu() {
        return anzahlNeu;
    }

    public int getAnzahlUebersprungen() {
        return anzahlUebersprungen;
    }

    public int getAnzahlIgnoriert() {
        return anzahlIgnoriert;
    }
}
