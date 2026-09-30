package ch.kmu.debitoren.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Eine Revision des Audit-Logs: wer wann für welchen Mandanten geändert hat. Hibernate Envers legt sie je
 * Transaktion an und schreibt den Stand jeder geänderten Entität in deren {@code _aud}-Tabelle.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(RevisionErfassung.class)
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    private long id;

    @RevisionTimestamp
    @Column(nullable = false)
    private Instant zeitpunkt;

    @Column(nullable = false)
    private String benutzer;

    /** Leer bei Änderungen ohne Mandant, etwa beim Anlegen eines Mandanten durch den Betrieb. */
    private UUID mandantId;

    protected Revision() {}

    void erfassen(String benutzer, UUID mandantId) {
        this.benutzer = benutzer;
        this.mandantId = mandantId;
    }

    public long getId() {
        return id;
    }

    public Instant getZeitpunkt() {
        return zeitpunkt;
    }

    public String getBenutzer() {
        return benutzer;
    }

    public UUID getMandantId() {
        return mandantId;
    }
}
