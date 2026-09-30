package ch.kmu.debitoren.rechnung.domain;

import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.NORMAL_2018;
import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.NORMAL_2024;
import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.REDUZIERT_2024;
import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.rechnung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.fehler.Fehlercode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RechnungTest {

    private static final Instant JETZT = Instant.parse("2026-09-30T08:00:00Z");
    private static final QrReferenz REFERENZ = QrReferenz.aus("2026000001");

    @Test
    void entwurfBerechnetBetraegeUndFaelligkeit() {
        var rechnung = rechnung()
                .datum(LocalDate.of(2026, 9, 1))
                .frist(20)
                .position("Montage", "3", "120.00", NORMAL_2024)
                .position("Pflanzen", "10", "7.35", REDUZIERT_2024)
                .entwurf();

        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.ENTWURF);
        assertThat(rechnung.getFaelligAm()).isEqualTo(LocalDate.of(2026, 9, 21));
        assertThat(rechnung.getPositionen())
                .extracting(Rechnungsposition::getPosition)
                .containsExactly(1, 2);
        assertThat(rechnung.getPositionen().get(1).getBetrag()).isEqualByComparingTo("73.50");
        // 360.00 + 29.16 + 73.50 + 1.91 = 464.57 → 464.55
        var summen = rechnung.summen();
        assertThat(summen.netto()).isEqualTo(Geld.chf("433.50"));
        assertThat(summen.mwstTotal()).isEqualTo(Geld.chf("31.07"));
        assertThat(rechnung.getTotal()).isEqualTo(Geld.chf("464.55"));
        assertThat(summen.rundung()).isEqualTo(Geld.chf("-0.02"));
    }

    @Test
    void entwurfLaesstSichAendern() {
        var rechnung = rechnung().standardposition().entwurf();
        var neu = rechnung().position("Beratung", "1", "150.00", NORMAL_2024).bemerkung("Neu");

        rechnung.entwurfAendern(UUID.randomUUID(), neu.empfaenger(), neu.kopf(), neu.positionen(), true);

        assertThat(rechnung.getPositionen())
                .singleElement()
                .extracting(Rechnungsposition::getBeschreibung)
                .isEqualTo("Beratung");
        assertThat(rechnung.getBemerkung()).isEqualTo("Neu");
        assertThat(rechnung.getTotal()).isEqualTo(Geld.chf("162.15"));
    }

    @Test
    void freigabeVergibtNummerUndReferenz() {
        var rechnung = rechnung().standardposition().entwurf();
        var neuerEmpfaenger = new Empfaenger("Keller AG", Adresse.schweiz("Neuweg", "1", "3000", "Bern"));

        rechnung.freigeben("RE-2026-0001", REFERENZ, neuerEmpfaenger, JETZT);

        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.OFFEN);
        assertThat(rechnung.getRechnungsnummer()).isEqualTo("RE-2026-0001");
        assertThat(rechnung.getReferenz()).isEqualTo(REFERENZ);
        assertThat(rechnung.getFreigegebenAm()).isEqualTo(JETZT);
        assertThat(rechnung.getEmpfaenger()).isEqualTo(neuerEmpfaenger);
    }

    @Test
    void freigegebeneRechnungIstUnveraenderlich() {
        var rechnung = freigegeben();
        var neu = rechnung().standardposition();

        erwarteFehler(
                () -> rechnung.entwurfAendern(UUID.randomUUID(), neu.empfaenger(), neu.kopf(), neu.positionen(), true),
                RechnungFehlercode.NUR_ENTWURF_AENDERBAR);
        erwarteFehler(
                () -> rechnung.freigeben("RE-2026-0002", REFERENZ, neu.empfaenger(), JETZT),
                RechnungFehlercode.FREIGABE_NICHT_MOEGLICH);
        erwarteFehler(rechnung::pruefeLoeschbar, RechnungFehlercode.NUR_ENTWURF_AENDERBAR);
    }

    @Test
    void ohnePositionenOderBetragKeineFreigabe() {
        var leer = rechnung().entwurf();
        erwarteFehler(
                () -> leer.freigeben("RE-2026-0001", REFERENZ, rechnung().empfaenger(), JETZT),
                RechnungFehlercode.FREIGABE_NICHT_MOEGLICH);

        var gratis = rechnung().position("Kulanz", "1", "0.00", NORMAL_2024).entwurf();
        erwarteFehler(
                () -> gratis.freigeben("RE-2026-0001", REFERENZ, rechnung().empfaenger(), JETZT),
                RechnungFehlercode.FREIGABE_NICHT_MOEGLICH);
    }

    @Test
    void stornoNurFuerOffeneRechnungen() {
        erwarteFehler(
                () -> rechnung().standardposition().entwurf().stornieren("Test", JETZT),
                RechnungFehlercode.STORNO_NICHT_MOEGLICH);

        var rechnung = freigegeben();
        rechnung.stornieren("Doppelt erfasst", JETZT);
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.STORNIERT);
        assertThat(rechnung.getStornoGrund()).isEqualTo("Doppelt erfasst");
        assertThat(rechnung.getStorniertAm()).isEqualTo(JETZT);

        erwarteFehler(() -> rechnung.stornieren("Nochmals", JETZT), RechnungFehlercode.STORNO_NICHT_MOEGLICH);
    }

    @Test
    void zahlungsstandFolgtDerSummeDerZuordnungen() {
        var rechnung = freigegeben();
        var total = rechnung.getTotal();

        rechnung.zahlungsstandSetzen(Geld.chf("50.00"));
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.TEILBEZAHLT);
        rechnung.zahlungsstandSetzen(total);
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.BEZAHLT);
        assertThat(rechnung.istUeberfaellig(rechnung.getFaelligAm().plusDays(30)))
                .isFalse();

        // Hebt jemand die Zuordnungen auf, ist die Rechnung wieder offen.
        rechnung.zahlungsstandSetzen(Geld.chf("0"));
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.OFFEN);

        erwarteFehler(
                () -> rechnung.zahlungsstandSetzen(total.plus(Geld.chf("0.05"))),
                RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH);
        erwarteFehler(() -> rechnung.zahlungsstandSetzen(Geld.eur("10.00")), RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH);
    }

    @Test
    void mahngebuehrenGehoerenZurForderung() {
        var rechnung = freigegeben();
        var total = rechnung.getTotal();

        rechnung.mahngebuehrBelasten(Geld.chf("20.00"));
        rechnung.mahngebuehrBelasten(Geld.chf("0.00"));
        assertThat(rechnung.getMahngebuehren()).isEqualTo(Geld.chf("20.00"));
        assertThat(rechnung.getForderung()).isEqualTo(total.plus(Geld.chf("20.00")));

        // Das Total allein bezahlt die Rechnung nicht mehr, erst zusammen mit der Gebühr.
        rechnung.zahlungsstandSetzen(total);
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.TEILBEZAHLT);
        rechnung.zahlungsstandSetzen(rechnung.getForderung());
        assertThat(rechnung.getStatus()).isEqualTo(Rechnungsstatus.BEZAHLT);

        erwarteFehler(() -> rechnung.mahngebuehrBelasten(Geld.chf("10.00")), RechnungFehlercode.MAHNUNG_NICHT_MOEGLICH);
    }

    @Test
    void mahngebuehrNurInDerWaehrungDerRechnungUndNieNegativ() {
        var rechnung = freigegeben();
        erwarteFehler(() -> rechnung.mahngebuehrBelasten(Geld.eur("20.00")), RechnungFehlercode.MAHNUNG_NICHT_MOEGLICH);
        assertThatThrownBy(() -> rechnung.mahngebuehrBelasten(Geld.chf("-5.00")))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("negativ");
        erwarteFehler(
                () -> rechnung().standardposition().entwurf().mahngebuehrBelasten(Geld.chf("20.00")),
                RechnungFehlercode.MAHNUNG_NICHT_MOEGLICH);
    }

    @Test
    void entwuerfeUndStornierteRechnungenNehmenKeineZahlungAn() {
        erwarteFehler(
                () -> rechnung().standardposition().entwurf().zahlungsstandSetzen(Geld.chf("10.00")),
                RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH);

        var storniert = freigegeben();
        storniert.stornieren("Doppelt erfasst", JETZT);
        erwarteFehler(
                () -> storniert.zahlungsstandSetzen(Geld.chf("10.00")), RechnungFehlercode.ZAHLUNG_NICHT_MOEGLICH);
    }

    @Test
    void stornoVerlangtEinenGrund() {
        var rechnung = freigegeben();
        assertThatThrownBy(() -> rechnung.stornieren(" ", JETZT)).isInstanceOf(FachlicherFehler.class);
    }

    @Test
    void ueberfaelligNurWennOffenUndFristVorbei() {
        var rechnung = freigegeben();
        var faellig = rechnung.getFaelligAm();

        assertThat(rechnung.istUeberfaellig(faellig)).isFalse();
        assertThat(rechnung.istUeberfaellig(faellig.plusDays(1))).isTrue();
        assertThat(rechnung().standardposition().entwurf().istUeberfaellig(faellig.plusDays(1)))
                .isFalse();
    }

    @Test
    void mwstSatzMussAmRechnungsdatumGelten() {
        var builder = rechnung().datum(LocalDate.of(2023, 12, 31)).position("Alt", "1", "10.00", NORMAL_2024);
        assertThatThrownBy(builder::entwurf)
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(RechnungFehlercode.MWST_SATZ_UNBEKANNT));

        var alt = rechnung()
                .datum(LocalDate.of(2023, 12, 31))
                .position("Alt", "1", "100.00", NORMAL_2018)
                .entwurf();
        assertThat(alt.getTotal()).isEqualTo(Geld.chf("107.70"));
    }

    @Test
    void nurChfUndEuro() {
        assertThatThrownBy(
                        () -> rechnung().waehrung(Currency.getInstance("USD")).kopf())
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(RechnungFehlercode.WAEHRUNG_NICHT_ERLAUBT));
        var euro = rechnung()
                .waehrung(Geld.EUR)
                .position("Service", "1", "10.01", NORMAL_2024)
                .entwurf();
        assertThat(euro.getTotal()).isEqualTo(Geld.eur("10.82"));
    }

    @Test
    void begrenztPositionenUndBetrag() {
        var viele = rechnung();
        for (int i = 0; i < 101; i++) {
            viele.standardposition();
        }
        assertThatThrownBy(viele::entwurf).isInstanceOf(FachlicherFehler.class).hasMessageContaining("100");

        var gross = rechnung().position("Grossauftrag", "999999", "9999.00", NORMAL_2024);
        assertThatThrownBy(gross::entwurf).isInstanceOf(FachlicherFehler.class).hasMessageContaining("999");
    }

    @Test
    void positionsdatenWerdenGeprueft() {
        assertThatThrownBy(() -> rechnung().position("X", "0", "1.00", NORMAL_2024))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Menge");
        assertThatThrownBy(() -> rechnung().position("X", "1.0001", "1.00", NORMAL_2024))
                .isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> rechnung().position("X", "1", "-1.00", NORMAL_2024))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Einzelpreis");
        assertThatThrownBy(() -> rechnung().position("X", "1", "1.001", NORMAL_2024))
                .isInstanceOf(FachlicherFehler.class);
        assertThatThrownBy(() -> rechnung().position(" ", "1", "1.00", NORMAL_2024))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageContaining("Beschreibung");
    }

    @Test
    void positionenHaltenDenSatzFest() {
        var position = rechnung().standardposition().entwurf().getPositionen().getFirst();
        assertThat(position.getMwstCode()).isEqualTo(MwstCode.NORMAL);
        assertThat(position.getMwstProzent()).isEqualByComparingTo("8.1");
        assertThat(position.getMenge()).isEqualByComparingTo(BigDecimal.TWO);
        assertThat(position.getEinheit()).isEqualTo("Std");
        assertThat(position.getEinzelpreis()).isEqualByComparingTo("100");
    }

    private static Rechnung freigegeben() {
        var rechnung = rechnung().standardposition().entwurf();
        rechnung.freigeben("RE-2026-0001", REFERENZ, rechnung().empfaenger(), JETZT);
        return rechnung;
    }

    private static void erwarteFehler(Runnable aktion, Fehlercode code) {
        assertThatThrownBy(aktion::run)
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(code));
    }
}
