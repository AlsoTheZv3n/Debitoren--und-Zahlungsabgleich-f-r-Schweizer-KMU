package ch.kmu.debitoren.rechnung.domain;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.modulith.NamedInterface;

/**
 * Rechnungskopf mit Positionen. Ein Entwurf ist frei änderbar; mit der Freigabe erhält die Rechnung Nummer
 * und Zahlungsreferenz und ist danach unveränderlich, bis sie bezahlt oder storniert wird.
 */
@NamedInterface("modell")
@Entity
@Table(name = "rechnung")
public class Rechnung {

    /** Grösster Betrag, den der QR-Zahlteil zulässt. */
    static final BigDecimal HOECHSTBETRAG = new BigDecimal("999999999.99");

    private static final int HOECHSTE_POSITIONSZAHL = 100;

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rechnungsstatus status;

    @Column(nullable = false)
    private UUID kundeId;

    private String rechnungsnummer;

    private Zahlungsreferenz referenz;

    @Column(nullable = false)
    private LocalDate rechnungsdatum;

    private int zahlungsfristTage;

    @Column(nullable = false)
    private LocalDate faelligAm;

    @Column(nullable = false)
    private Currency waehrung;

    private String bemerkung;

    @Column(nullable = false)
    private String empfaengerName;

    @Embedded
    @AttributeOverride(name = "strasse", column = @Column(name = "empfaenger_strasse"))
    @AttributeOverride(name = "hausnummer", column = @Column(name = "empfaenger_hausnummer"))
    @AttributeOverride(name = "plz", column = @Column(name = "empfaenger_plz"))
    @AttributeOverride(name = "ort", column = @Column(name = "empfaenger_ort"))
    @AttributeOverride(name = "land", column = @Column(name = "empfaenger_land"))
    private Adresse empfaengerAdresse;

    @Column(nullable = false)
    private BigDecimal netto;

    @Column(nullable = false)
    private BigDecimal mwstBetrag;

    @Column(nullable = false)
    private BigDecimal rundung;

    @Column(nullable = false)
    private BigDecimal total;

    private Instant freigegebenAm;

    private Instant storniertAm;

    private String stornoGrund;

    /** Technischer Verweis auf das abgelegte PDF; ändert die Version nicht, stört also keine Bearbeitung. */
    @OptimisticLock(excluded = true)
    private String pdfSchluessel;

    @CreationTimestamp
    private Instant erstelltAm;

    @UpdateTimestamp
    private Instant geaendertAm;

    @OneToMany(mappedBy = "rechnung", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Rechnungsposition> positionen = new ArrayList<>();

    protected Rechnung() {}

    private Rechnung(UUID kundeId) {
        this.id = Ids.neu();
        this.status = Rechnungsstatus.ENTWURF;
        this.kundeId = Pruefe.vorhanden(kundeId, "Kunde");
    }

    public static Rechnung entwurf(
            UUID kundeId,
            Empfaenger empfaenger,
            Rechnungskopf kopf,
            List<Positionsdaten> positionen,
            boolean rundungFuenfRappen) {
        var rechnung = new Rechnung(kundeId);
        rechnung.inhaltSetzen(empfaenger, kopf, positionen, rundungFuenfRappen);
        return rechnung;
    }

    public void entwurfAendern(
            UUID kundeId,
            Empfaenger empfaenger,
            Rechnungskopf kopf,
            List<Positionsdaten> positionen,
            boolean rundungFuenfRappen) {
        if (status != Rechnungsstatus.ENTWURF) {
            throw new FachlicherFehler(
                    RechnungFehlercode.NUR_ENTWURF_AENDERBAR,
                    "Rechnung %s ist freigegeben und lässt sich nicht mehr ändern.".formatted(rechnungsnummer));
        }
        this.kundeId = Pruefe.vorhanden(kundeId, "Kunde");
        inhaltSetzen(empfaenger, kopf, positionen, rundungFuenfRappen);
    }

    /**
     * Gibt den Entwurf frei. Nummer und Referenz vergibt der Aufrufer fortlaufend; der Empfänger wird noch
     * einmal aus den aktuellen Kundendaten übernommen.
     */
    public void freigeben(String nummer, Zahlungsreferenz referenz, Empfaenger empfaenger, Instant jetzt) {
        if (status != Rechnungsstatus.ENTWURF) {
            throw new FachlicherFehler(
                    RechnungFehlercode.FREIGABE_NICHT_MOEGLICH,
                    "Rechnung %s ist bereits freigegeben.".formatted(rechnungsnummer));
        }
        if (positionen.isEmpty() || total.signum() <= 0) {
            throw new FachlicherFehler(
                    RechnungFehlercode.FREIGABE_NICHT_MOEGLICH,
                    "Eine Rechnung braucht mindestens eine Position und einen Betrag über null.");
        }
        this.rechnungsnummer = Pruefe.pflichttext(nummer, "Rechnungsnummer", 20);
        this.referenz = Pruefe.vorhanden(referenz, "Referenz");
        empfaengerSetzen(empfaenger);
        this.freigegebenAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        this.status = Rechnungsstatus.OFFEN;
    }

    /** Storniert eine freigegebene, noch unbezahlte Rechnung. Entwürfe werden gelöscht, nicht storniert. */
    public void stornieren(String grund, Instant jetzt) {
        if (status != Rechnungsstatus.OFFEN) {
            throw new FachlicherFehler(
                    RechnungFehlercode.STORNO_NICHT_MOEGLICH,
                    switch (status) {
                        case ENTWURF -> "Ein Entwurf wird gelöscht, nicht storniert.";
                        case STORNIERT -> "Die Rechnung ist bereits storniert.";
                        default -> "Rechnungen mit Zahlungseingang lassen sich nicht stornieren.";
                    });
        }
        this.stornoGrund = Pruefe.pflichttext(grund, "Grund", 200);
        this.storniertAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        this.status = Rechnungsstatus.STORNIERT;
    }

    /**
     * Setzt den Zahlungsstand aus der Summe aller Zuordnungen: nichts bezahlt heisst offen, weniger als das
     * Total teilbezahlt, das ganze Total bezahlt. Heben Zuordnungen sich auf, geht der Status zurück.
     */
    public void zahlungsstandSetzen(Geld bezahlt) {
        Pruefe.vorhanden(bezahlt, "Bezahlter Betrag");
        if (status == Rechnungsstatus.ENTWURF || status == Rechnungsstatus.STORNIERT) {
            throw new FachlicherFehler(
                    RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH,
                    status == Rechnungsstatus.STORNIERT
                            ? "Rechnung %s ist storniert und nimmt keine Zahlung an.".formatted(rechnungsnummer)
                            : "Ein Entwurf nimmt keine Zahlung an.");
        }
        if (!bezahlt.waehrung().equals(waehrung)) {
            throw new FachlicherFehler(
                    RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH,
                    "Eine Zahlung in %s passt nicht zu Rechnung %s in %s."
                            .formatted(bezahlt.waehrung(), rechnungsnummer, waehrung));
        }
        var gesamt = getTotal();
        if (bezahlt.istNegativ() || bezahlt.istGroesserAls(gesamt)) {
            throw new FachlicherFehler(
                    RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH,
                    "Zugeordnet wären %s, Rechnung %s lautet auf %s.".formatted(bezahlt, rechnungsnummer, gesamt));
        }
        if (bezahlt.istNull()) {
            this.status = Rechnungsstatus.OFFEN;
        } else if (bezahlt.istKleinerAls(gesamt)) {
            this.status = Rechnungsstatus.TEILBEZAHLT;
        } else {
            this.status = Rechnungsstatus.BEZAHLT;
        }
    }

    public void pruefeLoeschbar() {
        if (status != Rechnungsstatus.ENTWURF) {
            throw new FachlicherFehler(RechnungFehlercode.NUR_ENTWURF_AENDERBAR, "Nur Entwürfe lassen sich löschen.");
        }
    }

    public void pdfAbgelegt(String schluessel) {
        this.pdfSchluessel = Pruefe.pflichttext(schluessel, "PDF-Schlüssel", 200);
    }

    public boolean istUeberfaellig(LocalDate heute) {
        return status.istOffen() && faelligAm.isBefore(heute);
    }

    /** Beträge je MWST-Satz, aus den festgehaltenen Positionen neu berechnet. */
    public Summen summen() {
        var berechnet = Rechnungsrechner.berechnen(
                positionen.stream().map(Rechnungsposition::alsPosten).toList(), waehrung, false);
        return new Summen(
                berechnet.netto(),
                berechnet.mwst(),
                berechnet.mwstTotal(),
                new Geld(rundung, waehrung),
                new Geld(total, waehrung));
    }

    private void inhaltSetzen(
            Empfaenger empfaenger, Rechnungskopf kopf, List<Positionsdaten> daten, boolean rundungFuenfRappen) {
        Pruefe.vorhanden(kopf, "Rechnungskopf");
        Pruefe.vorhanden(daten, "Positionen");
        if (daten.size() > HOECHSTE_POSITIONSZAHL) {
            throw Pruefe.ungueltig("Eine Rechnung hat höchstens %d Positionen".formatted(HOECHSTE_POSITIONSZAHL));
        }
        for (var position : daten) {
            if (!position.mwstSatz().giltAm(kopf.rechnungsdatum())) {
                throw new FachlicherFehler(
                        RechnungFehlercode.MWST_SATZ_UNBEKANNT,
                        "Der MWST-Satz %s gilt nicht am Rechnungsdatum."
                                .formatted(position.mwstSatz().getCode()));
            }
        }
        empfaengerSetzen(empfaenger);
        this.rechnungsdatum = kopf.rechnungsdatum();
        this.zahlungsfristTage = kopf.zahlungsfristTage();
        this.faelligAm = kopf.faelligAm();
        this.waehrung = kopf.waehrung();
        this.bemerkung = kopf.bemerkung();

        positionen.clear();
        for (int i = 0; i < daten.size(); i++) {
            positionen.add(new Rechnungsposition(this, i + 1, daten.get(i)));
        }
        var summen = Rechnungsrechner.berechnen(
                positionen.stream().map(Rechnungsposition::alsPosten).toList(), waehrung, rundungFuenfRappen);
        if (summen.total().betrag().compareTo(HOECHSTBETRAG) > 0) {
            throw Pruefe.ungueltig("Der QR-Zahlteil erlaubt höchstens 999’999’999.99");
        }
        this.netto = summen.netto().betrag();
        this.mwstBetrag = summen.mwstTotal().betrag();
        this.rundung = summen.rundung().betrag();
        this.total = summen.total().betrag();
    }

    private void empfaengerSetzen(Empfaenger empfaenger) {
        Pruefe.vorhanden(empfaenger, "Empfänger");
        this.empfaengerName = empfaenger.name();
        this.empfaengerAdresse = empfaenger.adresse();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMandantId() {
        return mandantId;
    }

    public Long getVersion() {
        return version;
    }

    public Rechnungsstatus getStatus() {
        return status;
    }

    public UUID getKundeId() {
        return kundeId;
    }

    public String getRechnungsnummer() {
        return rechnungsnummer;
    }

    public Zahlungsreferenz getReferenz() {
        return referenz;
    }

    public LocalDate getRechnungsdatum() {
        return rechnungsdatum;
    }

    public int getZahlungsfristTage() {
        return zahlungsfristTage;
    }

    public LocalDate getFaelligAm() {
        return faelligAm;
    }

    public Currency getWaehrung() {
        return waehrung;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public Empfaenger getEmpfaenger() {
        return new Empfaenger(empfaengerName, empfaengerAdresse);
    }

    public Geld getTotal() {
        return new Geld(total, waehrung);
    }

    public Instant getFreigegebenAm() {
        return freigegebenAm;
    }

    public Instant getStorniertAm() {
        return storniertAm;
    }

    public String getStornoGrund() {
        return stornoGrund;
    }

    public String getPdfSchluessel() {
        return pdfSchluessel;
    }

    public List<Rechnungsposition> getPositionen() {
        return Collections.unmodifiableList(positionen);
    }
}
