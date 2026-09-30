package ch.kmu.debitoren.rechnung.domain;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.envers.Audited;

/** MWST-Satz mit Gültigkeitszeitraum; ein neuer Satz ab einem Stichtag ist ein neuer Datensatz. */
@Entity
@Audited
@Table(name = "mwst_satz")
public class MwstSatz {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MwstCode code;

    @Column(nullable = false)
    private String bezeichnung;

    @Column(nullable = false)
    private BigDecimal satz;

    @Column(nullable = false)
    private LocalDate gueltigAb;

    private LocalDate gueltigBis;

    protected MwstSatz() {}

    public MwstSatz(MwstCode code, String bezeichnung, BigDecimal satz, LocalDate gueltigAb, LocalDate gueltigBis) {
        this.id = Ids.neu();
        this.code = Pruefe.vorhanden(code, "MWST-Code");
        this.bezeichnung = Pruefe.pflichttext(bezeichnung, "Bezeichnung", 60);
        Pruefe.vorhanden(satz, "Satz");
        if (satz.signum() < 0 || satz.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw Pruefe.ungueltig("Der MWST-Satz liegt zwischen 0 und 100 Prozent");
        }
        if (satz.stripTrailingZeros().scale() > 2) {
            throw Pruefe.ungueltig("Der MWST-Satz hat höchstens zwei Nachkommastellen");
        }
        this.satz = satz.setScale(2, RoundingMode.UNNECESSARY);
        this.gueltigAb = Pruefe.vorhanden(gueltigAb, "Gültig ab");
        if (gueltigBis != null && gueltigBis.isBefore(gueltigAb)) {
            throw Pruefe.ungueltig("Gültig bis liegt vor Gültig ab");
        }
        this.gueltigBis = gueltigBis;
    }

    /** Beendet den Satz, weil ab dem Folgetag ein neuer gilt. */
    public void beenden(LocalDate letzterTag) {
        Pruefe.vorhanden(letzterTag, "Gültig bis");
        if (letzterTag.isBefore(gueltigAb)) {
            throw Pruefe.ungueltig(
                    "Der neue Satz muss nach dem Beginn des bisherigen Satzes (%s) beginnen".formatted(gueltigAb));
        }
        this.gueltigBis = letzterTag;
    }

    public boolean giltAm(LocalDate datum) {
        return !datum.isBefore(gueltigAb) && (gueltigBis == null || !datum.isAfter(gueltigBis));
    }

    public UUID getId() {
        return id;
    }

    public MwstCode getCode() {
        return code;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public BigDecimal getSatz() {
        return satz;
    }

    public LocalDate getGueltigAb() {
        return gueltigAb;
    }

    public LocalDate getGueltigBis() {
        return gueltigBis;
    }
}
