package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Position einer Rechnung. Code und Prozentsatz der MWST werden festgehalten, damit ein späterer neuer Satz
 * eine bestehende Rechnung nie verändert.
 */
@Entity
@Table(name = "rechnungsposition")
public class Rechnungsposition {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rechnung_id", nullable = false, updatable = false)
    private Rechnung rechnung;

    private int position;

    @Column(nullable = false)
    private String beschreibung;

    @Column(nullable = false)
    private BigDecimal menge;

    private String einheit;

    @Column(nullable = false)
    private BigDecimal einzelpreis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mwst_satz_id", nullable = false)
    private MwstSatz mwstSatz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MwstCode mwstCode;

    @Column(nullable = false)
    private BigDecimal mwstProzent;

    @Column(nullable = false)
    private BigDecimal betrag;

    protected Rechnungsposition() {}

    Rechnungsposition(Rechnung rechnung, int position, Positionsdaten daten) {
        this.id = Ids.neu();
        this.rechnung = rechnung;
        this.position = position;
        this.beschreibung = daten.beschreibung();
        this.menge = daten.menge().setScale(3, RoundingMode.UNNECESSARY);
        this.einheit = daten.einheit();
        this.einzelpreis = daten.einzelpreis().setScale(2, RoundingMode.UNNECESSARY);
        this.mwstSatz = daten.mwstSatz();
        this.mwstCode = daten.mwstSatz().getCode();
        this.mwstProzent = daten.mwstSatz().getSatz();
        this.betrag = Rechnungsrechner.positionsbetrag(menge, einzelpreis);
    }

    Rechnungsrechner.Posten alsPosten() {
        return new Rechnungsrechner.Posten(betrag, mwstCode, mwstProzent);
    }

    public int getPosition() {
        return position;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public BigDecimal getMenge() {
        return menge;
    }

    public String getEinheit() {
        return einheit;
    }

    public BigDecimal getEinzelpreis() {
        return einzelpreis;
    }

    public MwstCode getMwstCode() {
        return mwstCode;
    }

    public BigDecimal getMwstProzent() {
        return mwstProzent;
    }

    public BigDecimal getBetrag() {
        return betrag;
    }
}
