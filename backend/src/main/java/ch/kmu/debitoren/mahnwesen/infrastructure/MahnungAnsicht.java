package ch.kmu.debitoren.mahnwesen.infrastructure;

import ch.kmu.debitoren.mahnwesen.domain.MahnungZeile;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.mahnwesen.domain.Versandweg;
import ch.kmu.debitoren.shared.domain.Geld;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/** Nur lesbare Zeile der Sicht {@code v_mahnungen}; der heute offene Betrag stammt aus den offenen Posten. */
@Entity
@Immutable
@Table(name = "v_mahnungen")
class MahnungAnsicht {

    @Id
    private UUID mahnungId;

    @TenantId
    @Column(updatable = false)
    private UUID mandantId;

    private long version;

    private UUID rechnungId;

    private String rechnungsnummer;

    private UUID kundeId;

    private String kundeName;

    private LocalDate faelligAm;

    private int stufe;

    private String bezeichnung;

    private LocalDate mahndatum;

    private LocalDate fristBis;

    private BigDecimal gebuehr;

    private Currency waehrung;

    @Enumerated(EnumType.STRING)
    private Mahnungsstatus status;

    private BigDecimal offenerBetrag;

    private BigDecimal aktuellOffen;

    @Enumerated(EnumType.STRING)
    private Versandweg versandweg;

    private String empfaengerEmail;

    private Instant freigegebenAm;

    private String freigegebenVon;

    private Instant versendetAm;

    private String versandfehler;

    private Instant verworfenAm;

    private String verworfenVon;

    private String verworfenGrund;

    private LocalDate gesperrtBis;

    private Instant erstelltAm;

    protected MahnungAnsicht() {}

    MahnungZeile alsZeile() {
        return new MahnungZeile(
                mahnungId,
                version,
                rechnungId,
                rechnungsnummer,
                kundeId,
                kundeName,
                faelligAm,
                stufe,
                bezeichnung,
                mahndatum,
                fristBis,
                new Geld(gebuehr, waehrung),
                status,
                offenerBetrag == null ? null : new Geld(offenerBetrag, waehrung),
                new Geld(aktuellOffen, waehrung),
                versandweg,
                empfaengerEmail,
                freigegebenAm,
                freigegebenVon,
                versendetAm,
                versandfehler,
                verworfenAm,
                verworfenVon,
                verworfenGrund,
                gesperrtBis,
                erstelltAm);
    }
}
