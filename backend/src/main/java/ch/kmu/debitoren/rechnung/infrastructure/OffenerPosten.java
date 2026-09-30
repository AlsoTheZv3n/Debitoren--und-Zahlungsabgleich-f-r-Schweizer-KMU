package ch.kmu.debitoren.rechnung.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/** Nur lesbare Zeile der Sicht {@code v_offene_posten}; der offene Betrag wird nie gespeichert. */
@Entity
@Immutable
@Table(name = "v_offene_posten")
class OffenerPosten {

    @Id
    private UUID rechnungId;

    @TenantId
    @Column(updatable = false)
    private UUID mandantId;

    private BigDecimal offenerBetrag;

    private int tageUeberFaelligkeit;

    protected OffenerPosten() {}

    BigDecimal getOffenerBetrag() {
        return offenerBetrag;
    }

    int getTageUeberFaelligkeit() {
        return tageUeberFaelligkeit;
    }
}
