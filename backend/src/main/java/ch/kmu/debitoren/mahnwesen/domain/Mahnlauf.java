package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;
import org.springframework.modulith.NamedInterface;

/** Protokoll eines Mahnlaufs: wann, von wem, und was er gefunden hat. */
@NamedInterface("modell")
@Entity
@Immutable
@Table(name = "mahnlauf")
public class Mahnlauf {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Column(nullable = false)
    private LocalDate stichtag;

    @Column(nullable = false)
    private Instant ausgefuehrtAm;

    @Column(nullable = false)
    private String ausgeloestVon;

    /** Überfällige Rechnungen, die der Lauf geprüft hat. */
    private int geprueft;

    /** Neue Vorschläge. */
    private int vorschlaege;

    /** Alte Vorschläge, die der Lauf verworfen hat, weil die Rechnung nicht mehr offen ist. */
    private int verworfen;

    protected Mahnlauf() {}

    public Mahnlauf(
            LocalDate stichtag,
            Instant ausgefuehrtAm,
            String ausgeloestVon,
            int geprueft,
            int vorschlaege,
            int verworfen) {
        this.id = Ids.neu();
        this.stichtag = Pruefe.vorhanden(stichtag, "Stichtag");
        this.ausgefuehrtAm = Pruefe.vorhanden(ausgefuehrtAm, "Zeitpunkt");
        this.ausgeloestVon = Pruefe.pflichttext(ausgeloestVon, "Benutzer", 100);
        this.geprueft = geprueft;
        this.vorschlaege = vorschlaege;
        this.verworfen = verworfen;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getStichtag() {
        return stichtag;
    }

    public Instant getAusgefuehrtAm() {
        return ausgefuehrtAm;
    }

    public String getAusgeloestVon() {
        return ausgeloestVon;
    }

    public int getGeprueft() {
        return geprueft;
    }

    public int getVorschlaege() {
        return vorschlaege;
    }

    public int getVerworfen() {
        return verworfen;
    }
}
