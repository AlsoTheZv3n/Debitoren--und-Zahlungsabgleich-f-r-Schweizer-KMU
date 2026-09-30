package ch.kmu.debitoren.mahnwesen.domain;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.domain.Pruefe;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
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
import org.hibernate.annotations.TenantId;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Eine Mahnung zu einer Rechnung, vom Vorschlag des Mahnlaufs bis zum Versand. Rechnungsnummer, Kunde und
 * Fälligkeit hält sie beim Vorschlag fest; die Beträge des Schreibens bei der Freigabe.
 */
@Entity
@Audited
@Table(name = "mahnung")
public class Mahnung {

    @Id
    private UUID id;

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID mandantId;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private UUID rechnungId;

    @Column(nullable = false, updatable = false)
    private String rechnungsnummer;

    @Column(nullable = false, updatable = false)
    private UUID kundeId;

    @Column(nullable = false, updatable = false)
    private String kundeName;

    @Column(nullable = false, updatable = false)
    private LocalDate faelligAm;

    @Column(nullable = false, updatable = false)
    private int stufe;

    @Column(nullable = false, updatable = false)
    private String bezeichnung;

    @Column(nullable = false, updatable = false)
    private LocalDate mahndatum;

    @Column(nullable = false, updatable = false)
    private LocalDate fristBis;

    @Column(nullable = false, updatable = false)
    private BigDecimal gebuehr;

    @Column(nullable = false, updatable = false)
    private Currency waehrung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mahnungsstatus status;

    private BigDecimal rechnungsbetrag;

    private BigDecimal bezahlt;

    private BigDecimal mahngebuehren;

    private BigDecimal offenerBetrag;

    /** Brieftext mit eingesetzten Platzhaltern, wie er im Schreiben steht. */
    private String brieftext;

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

    /** Technischer Verweis auf das abgelegte Schreiben; das Repository setzt ihn ohne Versionsprüfung. */
    @NotAudited
    @Column(insertable = false, updatable = false)
    private String pdfSchluessel;

    @Column(nullable = false, updatable = false)
    private Instant erstelltAm;

    protected Mahnung() {}

    /** Schlägt für die Rechnung die gegebene Stufe vor; Frist und Gebühr stammen aus der Stufe. */
    public static Mahnung vorschlag(MahnbareRechnung rechnung, Mahnstufe stufe, LocalDate mahndatum, Instant jetzt) {
        Pruefe.vorhanden(rechnung, "Rechnung");
        Pruefe.vorhanden(stufe, "Mahnstufe");
        var mahnung = new Mahnung();
        mahnung.id = Ids.neu();
        mahnung.rechnungId = Pruefe.vorhanden(rechnung.rechnungId(), "Rechnung");
        mahnung.rechnungsnummer = Pruefe.pflichttext(rechnung.rechnungsnummer(), "Rechnungsnummer", 20);
        mahnung.kundeId = Pruefe.vorhanden(rechnung.kundeId(), "Kunde");
        mahnung.kundeName = Pruefe.pflichttext(rechnung.kundeName(), "Kunde", 70);
        mahnung.faelligAm = Pruefe.vorhanden(rechnung.faelligAm(), "Fälligkeit");
        mahnung.stufe = stufe.getStufe();
        mahnung.bezeichnung = stufe.getBezeichnung();
        mahnung.mahndatum = Pruefe.vorhanden(mahndatum, "Mahndatum");
        mahnung.fristBis = mahndatum.plusDays(stufe.getFristTage());
        mahnung.waehrung = rechnung.offen().waehrung();
        mahnung.gebuehr = stufe.gebuehrIn(mahnung.waehrung).betrag();
        mahnung.status = Mahnungsstatus.VORSCHLAG;
        mahnung.erstelltAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        return mahnung;
    }

    /**
     * Gibt den Vorschlag frei und hält fest, was im Schreiben steht. Mit E-Mail-Adresse geht es per E-Mail,
     * sonst per Post.
     *
     * @param mahngebuehren alle Gebühren der Rechnung, auch die dieser Mahnung
     * @param offen was der Kunde bis zur Frist zahlen soll
     * @param text Brieftext mit eingesetzten Platzhaltern
     */
    public void freigeben(
            Geld rechnungsbetrag,
            Geld bezahlt,
            Geld mahngebuehren,
            Geld offen,
            String text,
            String email,
            String benutzer,
            Instant jetzt) {
        nurVorschlag("freigegeben");
        this.rechnungsbetrag = betrag(rechnungsbetrag, "Rechnungsbetrag");
        this.bezahlt = betrag(bezahlt, "Bezahlt");
        this.mahngebuehren = betrag(mahngebuehren, "Mahngebühren");
        this.offenerBetrag = betrag(offen, "Offener Betrag");
        this.brieftext = Pruefe.pflichttext(text, "Brieftext", 2000);
        this.empfaengerEmail = Pruefe.optionalerText(email, "E-Mail", 254);
        this.versandweg = empfaengerEmail == null ? Versandweg.POST : Versandweg.EMAIL;
        this.freigegebenVon = Pruefe.pflichttext(benutzer, "Benutzer", 100);
        this.freigegebenAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        this.status = Mahnungsstatus.FREIGEGEBEN;
    }

    /** Verwirft den Vorschlag; bis {@code gesperrtBis} schlägt der Mahnlauf die Rechnung nicht mehr vor. */
    public void verwerfen(String grund, LocalDate gesperrtBis, String benutzer, Instant jetzt) {
        nurVorschlag("verworfen");
        this.verworfenGrund = Pruefe.optionalerText(grund, "Grund", 200);
        if (gesperrtBis != null && gesperrtBis.isBefore(mahndatum)) {
            throw Pruefe.ungueltig("Die Sperre endet vor dem Mahndatum");
        }
        this.gesperrtBis = gesperrtBis;
        this.verworfenVon = Pruefe.pflichttext(benutzer, "Benutzer", 100);
        this.verworfenAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        this.status = Mahnungsstatus.VERWORFEN;
    }

    public void versendet(Instant jetzt) {
        if (status != Mahnungsstatus.FREIGEGEBEN) {
            throw new FachlicherFehler(
                    MahnwesenFehlercode.VERSAND_NICHT_MOEGLICH,
                    status == Mahnungsstatus.VERSENDET
                            ? "Die Mahnung ist bereits versendet."
                            : "Nur freigegebene Mahnungen werden versendet.");
        }
        this.versendetAm = Pruefe.vorhanden(jetzt, "Zeitpunkt");
        this.versandfehler = null;
        this.status = Mahnungsstatus.VERSENDET;
    }

    /** Der Mailserver hat die Mahnung nicht angenommen; sie bleibt freigegeben und lässt sich erneut senden. */
    public void versandFehlgeschlagen(String fehler) {
        if (status != Mahnungsstatus.FREIGEGEBEN) {
            throw new FachlicherFehler(
                    MahnwesenFehlercode.VERSAND_NICHT_MOEGLICH, "Nur freigegebene Mahnungen werden versendet.");
        }
        var text = fehler == null || fehler.isBlank() ? "Unbekannter Fehler" : fehler.strip();
        this.versandfehler = text.length() > 500 ? text.substring(0, 500) : text;
    }

    private void nurVorschlag(String aktion) {
        if (status != Mahnungsstatus.VORSCHLAG) {
            throw new FachlicherFehler(
                    MahnwesenFehlercode.NUR_VORSCHLAG_MOEGLICH,
                    "%s zu Rechnung %s ist kein Vorschlag mehr und kann nicht %s werden."
                            .formatted(bezeichnung, rechnungsnummer, aktion));
        }
    }

    private BigDecimal betrag(Geld geld, String feld) {
        Pruefe.vorhanden(geld, feld);
        if (!geld.waehrung().equals(waehrung)) {
            throw Pruefe.ungueltig("%s lautet auf %s, die Mahnung auf %s".formatted(feld, geld.waehrung(), waehrung));
        }
        if (geld.istNegativ()) {
            throw Pruefe.ungueltig(feld + " ist negativ");
        }
        return geld.betrag();
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getRechnungId() {
        return rechnungId;
    }

    public String getRechnungsnummer() {
        return rechnungsnummer;
    }

    public UUID getKundeId() {
        return kundeId;
    }

    public String getKundeName() {
        return kundeName;
    }

    public LocalDate getFaelligAm() {
        return faelligAm;
    }

    public int getStufe() {
        return stufe;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public LocalDate getMahndatum() {
        return mahndatum;
    }

    public LocalDate getFristBis() {
        return fristBis;
    }

    public Geld getGebuehr() {
        return new Geld(gebuehr, waehrung);
    }

    public Currency getWaehrung() {
        return waehrung;
    }

    public Mahnungsstatus getStatus() {
        return status;
    }

    public Geld getRechnungsbetrag() {
        return geld(rechnungsbetrag);
    }

    public Geld getBezahlt() {
        return geld(bezahlt);
    }

    public Geld getMahngebuehren() {
        return geld(mahngebuehren);
    }

    public Geld getOffenerBetrag() {
        return geld(offenerBetrag);
    }

    public String getBrieftext() {
        return brieftext;
    }

    public Versandweg getVersandweg() {
        return versandweg;
    }

    public String getEmpfaengerEmail() {
        return empfaengerEmail;
    }

    public Instant getFreigegebenAm() {
        return freigegebenAm;
    }

    public String getFreigegebenVon() {
        return freigegebenVon;
    }

    public Instant getVersendetAm() {
        return versendetAm;
    }

    public String getVersandfehler() {
        return versandfehler;
    }

    public Instant getVerworfenAm() {
        return verworfenAm;
    }

    public String getVerworfenGrund() {
        return verworfenGrund;
    }

    public LocalDate getGesperrtBis() {
        return gesperrtBis;
    }

    public String getPdfSchluessel() {
        return pdfSchluessel;
    }

    private Geld geld(BigDecimal betrag) {
        return betrag == null ? null : new Geld(betrag, waehrung);
    }
}
