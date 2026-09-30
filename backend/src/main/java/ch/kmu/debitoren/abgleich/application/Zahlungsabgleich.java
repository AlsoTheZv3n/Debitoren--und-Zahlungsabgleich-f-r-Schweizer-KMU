package ch.kmu.debitoren.abgleich.application;

import ch.kmu.debitoren.abgleich.domain.AbgleichFehlercode;
import ch.kmu.debitoren.abgleich.domain.Klaerungsgruende;
import ch.kmu.debitoren.abgleich.domain.OffenePostenAbfrage;
import ch.kmu.debitoren.abgleich.domain.OffenerPosten;
import ch.kmu.debitoren.abgleich.domain.Zahlungsfall;
import ch.kmu.debitoren.abgleich.domain.Zuordnung;
import ch.kmu.debitoren.abgleich.domain.ZuordnungRepository;
import ch.kmu.debitoren.abgleich.domain.ZuordnungsStrategie;
import ch.kmu.debitoren.abgleich.domain.ZuordnungsVorschlag;
import ch.kmu.debitoren.abgleich.domain.Zuordnungsart;
import ch.kmu.debitoren.rechnung.application.Zahlungseingaenge;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.security.AngemeldeterBenutzer;
import ch.kmu.debitoren.zahlungsimport.application.ZahlungInfo;
import ch.kmu.debitoren.zahlungsimport.application.Zahlungsstand;
import ch.kmu.debitoren.zahlungsimport.domain.Klaerungsgrund;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsstatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ordnet Zahlungen Rechnungen zu, automatisch nach dem Import oder von Hand in der Klärungsliste. Zuordnung,
 * Zahlungsstand der Rechnung und Status der Zahlung ändern sich in einer Transaktion.
 *
 * <p>Ergebnis einer Zuordnung: gleicher Betrag heisst bezahlt, ein kleinerer teilbezahlt; ein grösserer
 * bezahlt die Rechnung, der Überschuss bleibt in der Klärung.
 */
@Service
public class Zahlungsabgleich {

    private final List<ZuordnungsStrategie> strategien;
    private final ZuordnungRepository zuordnungen;
    private final OffenePostenAbfrage posten;
    private final Zahlungsstand zahlungsstand;
    private final Zahlungseingaenge zahlungseingaenge;
    private final Clock uhr;

    /** Die Strategien kommen in der Reihenfolge ihrer {@code @Order}. */
    Zahlungsabgleich(
            List<ZuordnungsStrategie> strategien,
            ZuordnungRepository zuordnungen,
            OffenePostenAbfrage posten,
            Zahlungsstand zahlungsstand,
            Zahlungseingaenge zahlungseingaenge,
            Clock uhr) {
        this.strategien = List.copyOf(strategien);
        this.zuordnungen = zuordnungen;
        this.posten = posten;
        this.zahlungsstand = zahlungsstand;
        this.zahlungseingaenge = zahlungseingaenge;
        this.uhr = uhr;
    }

    /**
     * Gleicht eine neu importierte Zahlung ab. Eine bereits bearbeitete Zahlung bleibt unberührt; das macht
     * einen wiederholten Aufruf, etwa nach einem Neustart, harmlos.
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public void abgleichen(UUID zahlungId) {
        MandantKontext.erforderlich();
        if (zahlungsstand.lesen(zahlungId).status() != Zahlungsstatus.OFFEN) {
            return;
        }
        var zahlung = zahlungsstand.sperren(zahlungId, null);
        var fall = new Zahlungsfall(zahlung.id(), zahlung.betrag(), zahlung.referenz(), zahlung.auftraggeber());
        for (var strategie : strategien) {
            var vorschlag = strategie.vorschlagen(fall);
            if (vorschlag.isPresent() && vorschlag.get().automatisch()) {
                verbuchen(
                        zahlung,
                        vorschlag.get().posten(),
                        vorschlag.get().betrag(),
                        Zuordnungsart.AUTO,
                        strategie.regel());
                return;
            }
        }
        zahlungsstand.inKlaerung(zahlungId, Klaerungsgruende.bestimmen(fall, posten.nachReferenz(fall.referenz())));
    }

    /**
     * Ordnet eine Zahlung von Hand einer Rechnung zu.
     *
     * @param version Version der Zahlung, die der Benutzer gesehen hat
     * @param betrag was zugeordnet wird; ohne Angabe der Rest der Zahlung, höchstens der offene Betrag
     */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public void zuordnen(UUID zahlungId, long version, UUID rechnungId, BigDecimal betrag) {
        MandantKontext.erforderlich();
        var zahlung = zahlungsstand.sperren(zahlungId, version);
        if (zahlung.status() == Zahlungsstatus.OFFEN) {
            throw new FachlicherFehler(
                    AbgleichFehlercode.ZAHLUNG_NOCH_IM_ABGLEICH,
                    "Der automatische Abgleich bearbeitet diese Zahlung noch. Bitte gleich nochmals versuchen.");
        }
        var rest = rest(zahlung);
        if (!rest.istPositiv()) {
            throw new FachlicherFehler(
                    AbgleichFehlercode.ZAHLUNG_BEREITS_ZUGEORDNET, "Die Zahlung ist bereits vollständig zugeordnet.");
        }
        var rechnung = posten.finde(rechnungId)
                .orElseThrow(() -> new FachlicherFehler(
                        AbgleichFehlercode.RECHNUNG_NICHT_GEFUNDEN,
                        "Rechnung %s nicht gefunden".formatted(rechnungId)));
        if (!rechnung.istZahlbar()) {
            throw new FachlicherFehler(
                    AbgleichFehlercode.ZUORDNUNG_NICHT_MOEGLICH,
                    "Rechnung %s ist %s und nimmt keine Zahlung mehr an."
                            .formatted(
                                    rechnung.rechnungsnummer(),
                                    rechnung.zustand().name().toLowerCase(Locale.ROOT)));
        }
        if (!rechnung.offen().waehrung().equals(rest.waehrung())) {
            throw new FachlicherFehler(
                    AbgleichFehlercode.ZUORDNUNG_NICHT_MOEGLICH,
                    "Die Zahlung lautet auf %s, Rechnung %s auf %s."
                            .formatted(
                                    rest.waehrung(),
                                    rechnung.rechnungsnummer(),
                                    rechnung.offen().waehrung()));
        }
        var zuzuordnen = betrag == null ? kleinerer(rest, rechnung.offen()) : new Geld(betrag, rest.waehrung());
        if (!zuzuordnen.istPositiv()
                || zuzuordnen.istGroesserAls(rest)
                || zuzuordnen.istGroesserAls(rechnung.offen())) {
            throw new FachlicherFehler(
                    AbgleichFehlercode.BETRAG_UNGUELTIG,
                    "Zugeordnet werden kann höchstens %s: der Rest der Zahlung und der offene Betrag der Rechnung."
                            .formatted(kleinerer(rest, rechnung.offen())));
        }
        verbuchen(zahlung, rechnung, zuzuordnen, Zuordnungsart.MANUELL, "MANUELL");
    }

    /** Hebt eine Zuordnung auf; Zahlung und Rechnung rechnen ihren Stand neu, die Zahlung geht in die Klärung. */
    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public void aufheben(UUID zuordnungId) {
        MandantKontext.erforderlich();
        var zuordnung = zuordnungen
                .finde(zuordnungId)
                .orElseThrow(() -> new FachlicherFehler(
                        AbgleichFehlercode.ZUORDNUNG_NICHT_GEFUNDEN,
                        "Zuordnung %s nicht gefunden".formatted(zuordnungId)));
        zahlungsstand.sperren(zuordnung.getZahlungId(), null);
        zuordnungen.loeschen(zuordnung);
        rechnungNeuRechnen(zuordnung.getRechnungId(), zuordnung.getBetrag());
        zahlungsstand.inKlaerung(zuordnung.getZahlungId(), Klaerungsgrund.ZUORDNUNG_AUFGEHOBEN);
    }

    /** Vorschläge aller Strategien für den Rest der Zahlung, je Rechnung nur einmal. */
    @PreAuthorize("hasRole('LESER')")
    @Transactional(readOnly = true)
    public List<ZuordnungsVorschlag> vorschlaege(UUID zahlungId) {
        MandantKontext.erforderlich();
        var zahlung = zahlungsstand.lesen(zahlungId);
        var rest = rest(zahlung);
        if (!rest.istPositiv()) {
            return List.of();
        }
        var fall = new Zahlungsfall(zahlung.id(), rest, zahlung.referenz(), zahlung.auftraggeber());
        var jeRechnung = new LinkedHashMap<UUID, ZuordnungsVorschlag>();
        strategien.stream()
                .map(strategie -> strategie.vorschlagen(fall))
                .flatMap(Optional::stream)
                .forEach(vorschlag -> jeRechnung.putIfAbsent(vorschlag.posten().rechnungId(), vorschlag));
        return List.copyOf(jeRechnung.values());
    }

    private void verbuchen(ZahlungInfo zahlung, OffenerPosten rechnung, Geld betrag, Zuordnungsart art, String regel) {
        zuordnungen.speichern(new Zuordnung(
                zahlung.id(),
                rechnung.rechnungId(),
                betrag,
                art,
                regel,
                AngemeldeterBenutzer.name(),
                Instant.now(uhr)));
        rechnungNeuRechnen(rechnung.rechnungId(), betrag);
        var rest = rest(zahlung);
        if (rest.istPositiv()) {
            zahlungsstand.inKlaerung(
                    zahlung.id(), art == Zuordnungsart.AUTO ? Klaerungsgrund.UEBERZAHLUNG : Klaerungsgrund.RESTBETRAG);
        } else {
            zahlungsstand.zugeordnet(zahlung.id());
        }
    }

    private void rechnungNeuRechnen(UUID rechnungId, Geld beispiel) {
        var bezahlt = zuordnungen.fuerRechnung(rechnungId).stream()
                .map(Zuordnung::getBetrag)
                .reduce(Geld.nullBetrag(beispiel.waehrung()), Geld::plus);
        zahlungseingaenge.zahlungsstandSetzen(rechnungId, bezahlt);
    }

    private Geld rest(ZahlungInfo zahlung) {
        return zuordnungen.fuerZahlung(zahlung.id()).stream()
                .map(Zuordnung::getBetrag)
                .reduce(zahlung.betrag(), Geld::minus);
    }

    private static Geld kleinerer(Geld a, Geld b) {
        return a.istGroesserAls(b) ? b : a;
    }
}
