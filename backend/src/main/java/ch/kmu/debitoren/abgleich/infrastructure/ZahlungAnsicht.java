package ch.kmu.debitoren.abgleich.infrastructure;

import ch.kmu.debitoren.abgleich.domain.ZahlungZeile;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/** Nur lesbare Zeile der Sicht {@code v_zahlungen}; der Rest wird aus den Zuordnungen berechnet. */
@Entity
@Immutable
@Table(name = "v_zahlungen")
class ZahlungAnsicht {

    @Id
    private UUID zahlungId;

    @TenantId
    @Column(updatable = false)
    private UUID mandantId;

    private long version;

    private UUID zahlungsdateiId;

    private String bankReferenz;

    private LocalDate buchungsdatum;

    private LocalDate valuta;

    private BigDecimal betrag;

    private Currency waehrung;

    private BigDecimal rest;

    private String referenz;

    private String auftraggeber;

    private String auftraggeberIban;

    private String mitteilung;

    @Enumerated(EnumType.STRING)
    private Zahlungsstatus status;

    @Enumerated(EnumType.STRING)
    private Klaerungsgrund klaerungsgrund;

    protected ZahlungAnsicht() {}

    ZahlungZeile alsZeile() {
        return new ZahlungZeile(
                zahlungId,
                version,
                zahlungsdateiId,
                bankReferenz,
                buchungsdatum,
                valuta,
                new Geld(betrag, waehrung),
                new Geld(rest, waehrung),
                referenz,
                auftraggeber,
                auftraggeberIban,
                mitteilung,
                status,
                klaerungsgrund);
    }
}
