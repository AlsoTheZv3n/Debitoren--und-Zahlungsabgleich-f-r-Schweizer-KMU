package ch.kmu.debitoren.zahlungsimport.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Eine Gutschrift aus einer Bankdatei. Die Angaben der Bank ändern sich nie; der Abgleich setzt nur
 * Status und Klärungsgrund. Wie viel davon schon Rechnungen zugeordnet ist, rechnet die Sicht
 * {@code v_zahlungen} aus den Zuordnungen.
 */
@Entity
@Table(name = "zahlung")
public class Zahlung {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private UUID zahlungsdateiId;

    @Column(nullable = false, updatable = false)
    private String bankReferenz;

    @Column(updatable = false)
    private LocalDate buchungsdatum;

    @Column(nullable = false, updatable = false)
    private LocalDate valuta;

    @Column(nullable = false, updatable = false)
    private BigDecimal betrag;

    @Column(nullable = false, updatable = false)
    private Currency waehrung;

    @Column(updatable = false)
    private String referenz;

    @Column(updatable = false)
    private String auftraggeber;

    @Column(updatable = false)
    private String auftraggeberIban;

    @Column(updatable = false)
    private String mitteilung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zahlungsstatus status;

    @Enumerated(EnumType.STRING)
    private Klaerungsgrund klaerungsgrund;

    @CreationTimestamp
    private Instant erstelltAm;

    @UpdateTimestamp
    private Instant geaendertAm;

    protected Zahlung() {}

    private Zahlung(UUID zahlungsdateiId, Gutschrift gutschrift) {
        this.id = Ids.neu();
        this.zahlungsdateiId = Pruefe.vorhanden(zahlungsdateiId, "Zahlungsdatei");
        Pruefe.vorhanden(gutschrift, "Gutschrift");
        this.bankReferenz = gutschrift.bankReferenz();
        this.buchungsdatum = gutschrift.buchungsdatum();
        this.valuta = gutschrift.valuta();
        this.betrag = gutschrift.betrag().betrag();
        this.waehrung = gutschrift.betrag().waehrung();
        this.referenz = gutschrift.referenz();
        this.auftraggeber = gutschrift.auftraggeber();
        this.auftraggeberIban = gutschrift.auftraggeberIban();
        this.mitteilung = gutschrift.mitteilung();
        this.status = Zahlungsstatus.OFFEN;
    }

    /** Neue, noch nicht abgeglichene Zahlung aus einer Gutschrift. */
    public static Zahlung aus(UUID zahlungsdateiId, Gutschrift gutschrift) {
        return new Zahlung(zahlungsdateiId, gutschrift);
    }

    /** Vollständig zugeordnet; ein früherer Klärungsgrund entfällt. */
    public void zugeordnet() {
        this.status = Zahlungsstatus.ZUGEORDNET;
        this.klaerungsgrund = null;
    }

    /** Ganz oder teilweise ungeklärt. */
    public void inKlaerung(Klaerungsgrund grund) {
        this.klaerungsgrund = Pruefe.vorhanden(grund, "Klärungsgrund");
        this.status = Zahlungsstatus.KLAERUNG;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getZahlungsdateiId() {
        return zahlungsdateiId;
    }

    public String getBankReferenz() {
        return bankReferenz;
    }

    public LocalDate getBuchungsdatum() {
        return buchungsdatum;
    }

    public LocalDate getValuta() {
        return valuta;
    }

    public Geld getBetrag() {
        return new Geld(betrag, waehrung);
    }

    public String getReferenz() {
        return referenz;
    }

    public String getAuftraggeber() {
        return auftraggeber;
    }

    public String getAuftraggeberIban() {
        return auftraggeberIban;
    }

    public String getMitteilung() {
        return mitteilung;
    }

    public Zahlungsstatus getStatus() {
        return status;
    }

    public Klaerungsgrund getKlaerungsgrund() {
        return klaerungsgrund;
    }
}
