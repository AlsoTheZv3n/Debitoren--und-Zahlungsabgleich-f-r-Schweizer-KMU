package ch.kmu.debitoren.abgleich.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/**
 * Ordnet einen Teil einer Zahlung einer Rechnung zu. Eine Zuordnung ändert sich nie; wer sie aufhebt,
 * löscht sie, und Zahlung wie Rechnung rechnen ihren Stand neu.
 */
@Entity
@Immutable
@Table(name = "zuordnung")
public class Zuordnung {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Column(nullable = false)
    private UUID zahlungId;

    @Column(nullable = false)
    private UUID rechnungId;

    @Column(nullable = false)
    private BigDecimal betrag;

    @Column(nullable = false)
    private Currency waehrung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zuordnungsart art;

    @Column(nullable = false)
    private String regel;

    @Column(nullable = false)
    private String erstelltVon;

    @Column(nullable = false)
    private Instant erstelltAm;

    protected Zuordnung() {}

    public Zuordnung(
            UUID zahlungId,
            UUID rechnungId,
            Geld betrag,
            Zuordnungsart art,
            String regel,
            String erstelltVon,
            Instant erstelltAm) {
        this.id = Ids.neu();
        this.zahlungId = Pruefe.vorhanden(zahlungId, "Zahlung");
        this.rechnungId = Pruefe.vorhanden(rechnungId, "Rechnung");
        Pruefe.vorhanden(betrag, "Betrag");
        if (!betrag.istPositiv()) {
            throw Pruefe.ungueltig("Eine Zuordnung hat einen Betrag über null");
        }
        this.betrag = betrag.betrag();
        this.waehrung = betrag.waehrung();
        this.art = Pruefe.vorhanden(art, "Art");
        this.regel = Pruefe.pflichttext(regel, "Regel", 40);
        this.erstelltVon = Pruefe.pflichttext(erstelltVon, "Benutzer", 100);
        this.erstelltAm = Pruefe.vorhanden(erstelltAm, "Zeitpunkt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getZahlungId() {
        return zahlungId;
    }

    public UUID getRechnungId() {
        return rechnungId;
    }

    public Geld getBetrag() {
        return new Geld(betrag, waehrung);
    }

    public Zuordnungsart getArt() {
        return art;
    }

    public String getRegel() {
        return regel;
    }

    public String getErstelltVon() {
        return erstelltVon;
    }

    public Instant getErstelltAm() {
        return erstelltAm;
    }
}
