package ch.kmu.debitoren.stammdaten.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.modulith.NamedInterface;

/** Debitor eines Mandanten mit strukturierter Adresse und eigener Zahlungsfrist. */
@NamedInterface("modell")
@Entity
@Table(name = "kunde")
public class Kunde {

    public static final int STANDARD_ZAHLUNGSFRIST_TAGE = 30;

    @Id
    private UUID id;

    /** Setzt Hibernate beim Einfügen aus dem {@code MandantKontext}; Abfragen filtern danach. */
    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Column(nullable = false)
    private String kundennummer;

    @Column(nullable = false)
    private String name;

    @Embedded
    private Adresse adresse;

    private String email;

    private int zahlungsfristTage;

    @CreationTimestamp
    private Instant erstelltAm;

    @UpdateTimestamp
    private Instant geaendertAm;

    protected Kunde() {}

    public Kunde(String kundennummer, String name, Adresse adresse, String email, int zahlungsfristTage) {
        this.id = Ids.neu();
        this.kundennummer = Pruefe.pflichttext(kundennummer, "Kundennummer", 20);
        this.name = Pruefe.pflichttext(name, "Name", 70);
        this.adresse = Pruefe.vorhanden(adresse, "Adresse");
        this.email = Pruefe.optionalerText(email, "E-Mail", 254);
        this.zahlungsfristTage = Pruefe.imBereich(zahlungsfristTage, "Zahlungsfrist", 0, 365);
    }

    /** Ändert Name, Adresse, E-Mail und Zahlungsfrist; die Kundennummer bleibt, sie steht auf Rechnungen. */
    public void aendern(String name, Adresse adresse, String email, int zahlungsfristTage) {
        this.name = Pruefe.pflichttext(name, "Name", 70);
        this.adresse = Pruefe.vorhanden(adresse, "Adresse");
        this.email = Pruefe.optionalerText(email, "E-Mail", 254);
        this.zahlungsfristTage = Pruefe.imBereich(zahlungsfristTage, "Zahlungsfrist", 0, 365);
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getMandantId() {
        return mandantId;
    }

    public String getKundennummer() {
        return kundennummer;
    }

    public String getName() {
        return name;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public String getEmail() {
        return email;
    }

    public int getZahlungsfristTage() {
        return zahlungsfristTage;
    }
}
