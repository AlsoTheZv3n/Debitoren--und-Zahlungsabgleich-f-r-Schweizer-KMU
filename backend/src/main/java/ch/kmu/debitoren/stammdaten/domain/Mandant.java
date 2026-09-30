package ch.kmu.debitoren.stammdaten.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.domain.Referenzart;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Firma, die Rechnungen stellt. Mit QR-IBAN nutzt sie die QR-Referenz, sonst die SCOR-Referenz. Die IBAN
 * selbst darf keine QR-IBAN sein.
 */
@Entity
@Audited
@Table(name = "mandant")
public class Mandant {

    private static final Pattern MWST_NUMMER = Pattern.compile("CHE-\\d{3}\\.\\d{3}\\.\\d{3}( MWST)?");

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Adresse adresse;

    @Column(nullable = false)
    private Iban iban;

    private Iban qrIban;

    private String mwstNummer;

    private String email;

    private boolean rundungFuenfRappen = true;

    @CreationTimestamp
    @NotAudited
    private Instant erstelltAm;

    @UpdateTimestamp
    @NotAudited
    private Instant geaendertAm;

    protected Mandant() {}

    public Mandant(UUID id, String name, Adresse adresse, Iban iban, Iban qrIban) {
        this.id = Pruefe.vorhanden(id, "ID");
        aendern(name, adresse, iban, qrIban);
    }

    /**
     * Ändert Name, Adresse und Konten. Bestehende Rechnungen behalten ihre Referenz; neue erhalten mit QR-IBAN eine
     * QR-Referenz, sonst eine SCOR-Referenz.
     */
    public void aendern(String name, Adresse adresse, Iban iban, Iban qrIban) {
        this.name = Pruefe.pflichttext(name, "Name", 70);
        this.adresse = Pruefe.vorhanden(adresse, "Adresse");
        Pruefe.vorhanden(iban, "IBAN");
        if (iban.istQrIban()) {
            throw Pruefe.ungueltig("Als IBAN ist eine QR-IBAN nicht erlaubt; sie gehört ins Feld QR-IBAN");
        }
        if (qrIban != null && !qrIban.istQrIban()) {
            throw Pruefe.ungueltig("Die QR-IBAN muss eine Institutsidentifikation von 30000 bis 31999 haben");
        }
        this.iban = iban;
        this.qrIban = qrIban;
    }

    /** Angaben, die auf jeder Rechnung stehen. */
    public void rechnungsangabenSetzen(String mwstNummer, String email, boolean rundungFuenfRappen) {
        var nummer = Pruefe.optionalerText(mwstNummer, "MWST-Nummer", 20);
        if (nummer != null && !MWST_NUMMER.matcher(nummer).matches()) {
            throw Pruefe.ungueltig("MWST-Nummer im Format CHE-123.456.789 MWST angeben");
        }
        this.mwstNummer = nummer;
        this.email = Pruefe.optionalerText(email, "E-Mail", 254);
        this.rundungFuenfRappen = rundungFuenfRappen;
    }

    public Referenzart getReferenzart() {
        return qrIban != null ? Referenzart.QR : Referenzart.SCOR;
    }

    /** Konto für den QR-Zahlteil: die QR-IBAN, falls vorhanden, sonst die IBAN. */
    public Iban getZahlungskonto() {
        return qrIban != null ? qrIban : iban;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public Iban getIban() {
        return iban;
    }

    public Iban getQrIban() {
        return qrIban;
    }

    public String getMwstNummer() {
        return mwstNummer;
    }

    public String getEmail() {
        return email;
    }

    public boolean isRundungFuenfRappen() {
        return rundungFuenfRappen;
    }

    public Instant getErstelltAm() {
        return erstelltAm;
    }

    public Instant getGeaendertAm() {
        return geaendertAm;
    }
}
