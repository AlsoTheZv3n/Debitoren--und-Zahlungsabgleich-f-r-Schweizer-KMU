package ch.kmu.debitoren.mahnwesen.application;

import ch.kmu.debitoren.mahnwesen.domain.Mahntext;
import ch.kmu.debitoren.mahnwesen.domain.Mahnung;
import ch.kmu.debitoren.mahnwesen.domain.MahnungRepository;
import ch.kmu.debitoren.mahnwesen.domain.Mahnungsstatus;
import ch.kmu.debitoren.mahnwesen.domain.Mahnversand;
import ch.kmu.debitoren.mahnwesen.domain.Mahnversand.Mahnmail;
import ch.kmu.debitoren.mahnwesen.domain.Mahnversand.VersandFehlgeschlagen;
import ch.kmu.debitoren.mahnwesen.domain.MahnwesenFehlercode;
import ch.kmu.debitoren.mahnwesen.domain.Versandweg;
import ch.kmu.debitoren.rechnung.application.Mahnschreiben;
import ch.kmu.debitoren.rechnung.application.OffenePostenAbfragen;
import ch.kmu.debitoren.rechnung.domain.Mahnangaben;
import ch.kmu.debitoren.shared.dokumente.DokumentBereitsAbgelegt;
import ch.kmu.debitoren.shared.dokumente.Dokumentablage;
import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreiben und Versand freigegebener Mahnungen. Das PDF entsteht einmal und wird unveränderlich abgelegt; per
 * E-Mail geht es als Anhang hinaus. Ohne E-Mail-Adresse des Kunden druckt die Buchhaltung es aus und bestätigt den
 * Postversand.
 */
@Service
public class Mahnungsversand {

    private static final Logger LOG = LoggerFactory.getLogger(Mahnungsversand.class);

    private final MahnungRepository mahnungen;
    private final Mahnschreiben mahnschreiben;
    private final OffenePostenAbfragen posten;
    private final Brieftexte brieftexte;
    private final Dokumentablage ablage;
    private final Mahnversand versand;
    private final MandantenAbfragen mandanten;
    private final Clock uhr;

    Mahnungsversand(
            MahnungRepository mahnungen,
            Mahnschreiben mahnschreiben,
            OffenePostenAbfragen posten,
            Brieftexte brieftexte,
            Dokumentablage ablage,
            Mahnversand versand,
            MandantenAbfragen mandanten,
            Clock uhr) {
        this.mahnungen = mahnungen;
        this.mahnschreiben = mahnschreiben;
        this.posten = posten;
        this.brieftexte = brieftexte;
        this.ablage = ablage;
        this.versand = versand;
        this.mandanten = mandanten;
        this.uhr = uhr;
    }

    /**
     * Nach der Freigabe: legt das Schreiben ab und sendet es per E-Mail. Scheitert der Versand, bleibt die
     * Mahnung mit dem Fehler freigegeben. Bereits versandte Mahnungen bleiben unberührt; ein wiederholter
     * Aufruf, etwa nach einem Neustart, ist also harmlos.
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Mahnung versenden(UUID id) {
        MandantKontext.erforderlich();
        var mahnung = mahnungen.finde(id).orElseThrow(() -> MahnungAbfragen.nichtGefunden(id));
        if (mahnung.getStatus() != Mahnungsstatus.FREIGEGEBEN) {
            return mahnung;
        }
        var pdf = abgelegtesSchreiben(mahnung);
        if (mahnung.getVersandweg() == Versandweg.EMAIL && mahnung.getVersandfehler() == null) {
            senden(mahnung, pdf);
        }
        return mahnung;
    }

    /** Neuer Versuch nach einem Fehler des Mailservers. */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Mahnung erneutSenden(UUID id, long version) {
        MandantKontext.erforderlich();
        var mahnung = laden(id, version);
        if (mahnung.getStatus() != Mahnungsstatus.FREIGEGEBEN || mahnung.getVersandweg() != Versandweg.EMAIL) {
            throw new FachlicherFehler(
                    MahnwesenFehlercode.VERSAND_NICHT_MOEGLICH,
                    "Nur freigegebene Mahnungen, die per E-Mail gehen, lassen sich erneut senden.");
        }
        senden(mahnung, abgelegtesSchreiben(mahnung));
        return mahnung;
    }

    /** Bestätigt, dass ein Schreiben ohne E-Mail-Adresse ausgedruckt und per Post versandt ist. */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public Mahnung perPostVersendet(UUID id, long version) {
        MandantKontext.erforderlich();
        var mahnung = laden(id, version);
        if (mahnung.getVersandweg() != Versandweg.POST) {
            throw new FachlicherFehler(
                    MahnwesenFehlercode.VERSAND_NICHT_MOEGLICH, "Diese Mahnung geht per E-Mail, nicht per Post.");
        }
        mahnung.versendet(Instant.now(uhr));
        return mahnung;
    }

    /**
     * Das Schreiben als PDF. Bei einem Vorschlag eine Vorschau mit dem heutigen Stand, die nirgends abgelegt wird;
     * danach das abgelegte Schreiben.
     */
    @PreAuthorize("hasRole('LESER')")
    @Transactional
    public Pdf pdf(UUID id) {
        MandantKontext.erforderlich();
        var mahnung = mahnungen.finde(id).orElseThrow(() -> MahnungAbfragen.nichtGefunden(id));
        var inhalt = switch (mahnung.getStatus()) {
            case VORSCHLAG -> vorschau(mahnung);
            case FREIGEGEBEN, VERSENDET -> abgelegtesSchreiben(mahnung);
            case VERWORFEN ->
                throw new FachlicherFehler(
                        MahnwesenFehlercode.VERSAND_NICHT_MOEGLICH, "Ein verworfener Vorschlag hat kein Schreiben.");
        };
        return new Pdf(dateiname(mahnung), inhalt);
    }

    private byte[] vorschau(Mahnung mahnung) {
        var rechnung =
                posten.lesen(mahnung.getRechnungId()).orElseThrow(() -> MahnungAbfragen.nichtGefunden(mahnung.getId()));
        var gebuehr = mahnung.getGebuehr();
        var zuZahlen = rechnung.offen().plus(gebuehr);
        return mahnschreiben.pdf(
                mahnung.getRechnungId(),
                new Mahnangaben(
                        mahnung.getBezeichnung(),
                        mahnung.getMahndatum(),
                        mahnung.getFristBis(),
                        brieftexte.fuer(mahnung, zuZahlen),
                        rechnung.total(),
                        rechnung.bezahlt(),
                        rechnung.mahngebuehren().plus(gebuehr),
                        zuZahlen));
    }

    private byte[] abgelegtesSchreiben(Mahnung mahnung) {
        if (mahnung.getPdfSchluessel() != null) {
            var abgelegt = ablage.lesen(mahnung.getPdfSchluessel());
            if (abgelegt.isPresent()) {
                return abgelegt.get();
            }
        }
        var schluessel = "mahnungen/%s/%s.pdf".formatted(MandantKontext.erforderlich(), mahnung.getId());
        var inhalt = mahnschreiben.pdf(
                mahnung.getRechnungId(),
                new Mahnangaben(
                        mahnung.getBezeichnung(),
                        mahnung.getMahndatum(),
                        mahnung.getFristBis(),
                        mahnung.getBrieftext(),
                        mahnung.getRechnungsbetrag(),
                        mahnung.getBezahlt(),
                        mahnung.getMahngebuehren(),
                        mahnung.getOffenerBetrag()));
        try {
            ablage.ablegen(schluessel, inhalt);
        } catch (DokumentBereitsAbgelegt e) {
            // Gleichzeitig erstellt, etwa durch Download und Versand: das zuerst abgelegte gilt.
            inhalt = ablage.lesen(schluessel).orElseThrow();
        }
        mahnungen.pdfAbgelegt(mahnung.getId(), schluessel);
        return inhalt;
    }

    private void senden(Mahnung mahnung, byte[] pdf) {
        var mandant = mandanten.aktueller();
        var betreff = "%s zu Rechnung %s".formatted(mahnung.getBezeichnung(), mahnung.getRechnungsnummer());
        var text = """
                Guten Tag

                Im Anhang erhalten Sie unsere %s zur Rechnung %s. Offen sind %s, zahlbar bis am %s.

                Haben Sie die Rechnung in der Zwischenzeit bezahlt, betrachten Sie diese Nachricht bitte als \
                gegenstandslos.

                Freundliche Grüsse
                %s
                """.formatted(
                        mahnung.getBezeichnung(),
                        mahnung.getRechnungsnummer(),
                        Mahntext.betrag(mahnung.getOffenerBetrag()),
                        Mahntext.datum(mahnung.getFristBis()),
                        mandant.name());
        try {
            versand.senden(new Mahnmail(
                    mahnung.getEmpfaengerEmail(),
                    mandant.email(),
                    mandant.name(),
                    betreff,
                    text,
                    dateiname(mahnung),
                    pdf));
            mahnung.versendet(Instant.now(uhr));
        } catch (VersandFehlgeschlagen fehler) {
            LOG.warn("Versand der Mahnung {} gescheitert", mahnung.getId(), fehler);
            mahnung.versandFehlgeschlagen(fehler.getMessage());
        }
    }

    private Mahnung laden(UUID id, long version) {
        var mahnung = mahnungen.finde(id).orElseThrow(() -> MahnungAbfragen.nichtGefunden(id));
        if (!Objects.equals(mahnung.getVersion(), version)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                    "Die Mahnung wurde inzwischen geändert. Bitte neu laden.");
        }
        return mahnung;
    }

    /** Etwa «RE-2026-0412-1-Mahnung.pdf»: nur Zeichen, die jedes Mailprogramm und Dateisystem verträgt. */
    static String dateiname(Mahnung mahnung) {
        var name = (mahnung.getRechnungsnummer() + "-" + mahnung.getBezeichnung())
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replaceAll("[^A-Za-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
        return name + ".pdf";
    }

    public record Pdf(String dateiname, byte[] inhalt) {}
}
