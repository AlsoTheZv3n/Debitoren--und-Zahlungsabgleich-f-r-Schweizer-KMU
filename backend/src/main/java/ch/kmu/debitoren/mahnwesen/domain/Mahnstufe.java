package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.envers.Audited;

/**
 * Eine Stufe des Mahnwesens, etwa «Zahlungserinnerung» oder «2. Mahnung». Der Mahnlauf schlägt sie vor, sobald
 * eine Rechnung lange genug überfällig ist und die vorherige Stufe weit genug zurückliegt.
 */
@Entity
@Audited
@Table(name = "mahnstufe")
public class Mahnstufe {

    private static final BigDecimal HOECHSTGEBUEHR = BigDecimal.valueOf(1000);

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private int stufe;

    @Column(nullable = false)
    private String bezeichnung;

    private int tageNachFaelligkeit;

    private int mindestabstandTage;

    private int fristTage;

    @Column(nullable = false)
    private BigDecimal gebuehr;

    @Column(nullable = false)
    private String textvorlage;

    protected Mahnstufe() {}

    public Mahnstufe(int stufe, Mahnstufendaten daten) {
        this.id = Ids.neu();
        this.stufe = Pruefe.imBereich(stufe, "Stufe", 1, Mahnstufendaten.HOECHSTE_STUFENZAHL);
        setzen(daten);
    }

    public void aendern(Mahnstufendaten daten) {
        setzen(daten);
    }

    private void setzen(Mahnstufendaten daten) {
        Pruefe.vorhanden(daten, "Mahnstufe");
        this.bezeichnung = Pruefe.pflichttext(daten.bezeichnung(), "Bezeichnung", 40);
        this.tageNachFaelligkeit = Pruefe.imBereich(daten.tageNachFaelligkeit(), "Tage nach Fälligkeit", 1, 365);
        // Die erste Stufe hat keine Vorgängerin; ein Abstand hätte keine Wirkung.
        this.mindestabstandTage =
                stufe == 1 ? 0 : Pruefe.imBereich(daten.mindestabstandTage(), "Mindestabstand", 0, 365);
        this.fristTage = Pruefe.imBereich(daten.fristTage(), "Zahlungsfrist", 1, 90);
        var betrag = Pruefe.vorhanden(daten.gebuehr(), "Gebühr");
        if (betrag.signum() < 0 || betrag.compareTo(HOECHSTGEBUEHR) > 0) {
            throw Pruefe.ungueltig("Die Gebühr liegt zwischen 0 und 1000");
        }
        if (betrag.stripTrailingZeros().scale() > 2) {
            throw Pruefe.ungueltig("Die Gebühr hat höchstens zwei Nachkommastellen");
        }
        this.gebuehr = betrag.setScale(2, RoundingMode.UNNECESSARY);
        this.textvorlage = Mahntext.pruefen(Pruefe.pflichttext(daten.textvorlage(), "Text", 2000));
    }

    /** Die Gebühr in der Währung der gemahnten Rechnung. */
    public Geld gebuehrIn(Currency waehrung) {
        return new Geld(gebuehr, waehrung);
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public int getStufe() {
        return stufe;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public int getTageNachFaelligkeit() {
        return tageNachFaelligkeit;
    }

    public int getMindestabstandTage() {
        return mindestabstandTage;
    }

    public int getFristTage() {
        return fristTage;
    }

    public BigDecimal getGebuehr() {
        return gebuehr;
    }

    public String getTextvorlage() {
        return textvorlage;
    }
}
