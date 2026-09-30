package ch.kmu.debitoren.zahlungsimport.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsimportFehlercode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Der Parser liest die Golden Files und lehnt alles ab, was kein sauberes camt.054.001.08 ist. */
class Camt054ParserTest {

    private static final Camt054Parser PARSER = new Camt054Parser();

    @Test
    void liestJedeTransaktionEinerSammelbuchungUndIgnoriertBelastungUndVormerkung() {
        var inhalt = PARSER.lesen(datei("sammelbuchung.xml"));

        assertThat(inhalt.nachrichtId()).isEqualTo("MSG-SAMMEL-20260915");
        assertThat(inhalt.erstelltAm()).isEqualTo(Instant.parse("2026-09-15T04:30:00Z"));
        assertThat(inhalt.konten()).containsExactly("CH4431999123000889012");
        assertThat(inhalt.ignoriert()).isEqualTo(2);
        assertThat(inhalt.gutschriften()).hasSize(3);

        var erste = inhalt.gutschriften().getFirst();
        assertThat(erste.bankReferenz()).isEqualTo("SAMMEL-1");
        assertThat(erste.betrag()).isEqualTo(Geld.chf("150.00"));
        assertThat(erste.valuta()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(erste.buchungsdatum()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(erste.referenz()).isEqualTo("000000000000000020260000018");
        assertThat(erste.auftraggeber()).isEqualTo("Keller Haustechnik AG");
        assertThat(erste.auftraggeberIban()).isEqualTo("CH5604835012345678009");
        assertThat(inhalt.gutschriften())
                .extracting(g -> g.betrag().betrag().toPlainString())
                .containsExactly("150.00", "250.00", "600.00");
    }

    @Test
    void liestScorReferenzUndMitteilungOhneReferenz() {
        assertThat(PARSER.lesen(datei("scor-referenz.xml"))
                        .gutschriften()
                        .getFirst()
                        .referenz())
                .isEqualTo("RF422026000001");

        var ohne = PARSER.lesen(datei("ohne-referenz.xml")).gutschriften().getFirst();
        assertThat(ohne.referenz()).isNull();
        assertThat(ohne.mitteilung()).isEqualTo("Rechnung Beratung August");
        assertThat(ohne.auftraggeber()).isEqualTo("GASSER TREUHAND");
    }

    @Test
    void liestFremdwaehrung() {
        assertThat(PARSER.lesen(datei("eur-auf-chf-rechnung.xml"))
                        .gutschriften()
                        .getFirst()
                        .betrag())
                .isEqualTo(Geld.eur("420.00"));
    }

    @Test
    void lehntExterneEntitaetenAb() {
        var xxe = text("exakte-zahlung.xml")
                .replace("<Document", "<!DOCTYPE Document [<!ENTITY geheim SYSTEM \"file:///etc/passwd\">]>\n<Document")
                .replace("<MsgId>MSG-EXAKT-20260915</MsgId>", "<MsgId>&geheim;</MsgId>");

        assertThatThrownBy(() -> PARSER.lesen(xxe.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(ZahlungsimportFehlercode.DATEI_UNGUELTIG))
                .hasMessageNotContaining("root:");
    }

    @Test
    void lehntAndereNachrichtenUndVersionenAb() {
        var camt053 = text("exakte-zahlung.xml").replace("camt.054.001.08", "camt.053.001.08");

        assertThatThrownBy(() -> PARSER.lesen(camt053.getBytes(StandardCharsets.UTF_8)))
                .hasMessage("Die Datei ist kein camt.054-Dokument der Version 001.08.");
    }

    @Test
    void lehntSchemaverletzungenMitZeilennummerAb() {
        var ohneRichtung = text("exakte-zahlung.xml").replaceFirst("<CdtDbtInd>CRDT</CdtDbtInd>", "");

        assertThatThrownBy(() -> PARSER.lesen(ohneRichtung.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FachlicherFehler.class)
                .hasMessageStartingWith("Die Datei entspricht nicht dem Schema camt.054.001.08: Zeile ");
    }

    @Test
    void lehntLeereDateienUndKeinXmlAb() {
        assertThatThrownBy(() -> PARSER.lesen(new byte[0])).hasMessage("Die Datei ist leer.");
        assertThatThrownBy(() -> PARSER.lesen("Hallo".getBytes(StandardCharsets.UTF_8)))
                .hasMessageStartingWith("Die Datei ist kein gültiges XML");
    }

    @Test
    void lehntBetraegeMitMehrAlsZweiNachkommastellenAb() {
        var krumm = text("exakte-zahlung.xml").replace(">1840.00<", ">1840.005<");

        assertThatThrownBy(() -> PARSER.lesen(krumm.getBytes(StandardCharsets.UTF_8)))
                .hasMessageContaining("1840.005");
    }

    @Test
    void verlangtEineBankreferenz() {
        var ohne = text("exakte-zahlung.xml")
                .replace("<AcctSvcrRef>EXAKT-NTRY-1</AcctSvcrRef>", "")
                .replace("<AcctSvcrRef>EXAKT-1</AcctSvcrRef>", "");

        assertThatThrownBy(() -> PARSER.lesen(ohne.getBytes(StandardCharsets.UTF_8)))
                .hasMessage("Eine Buchung hat keine Bankreferenz (AcctSvcrRef).");
    }

    @Test
    void nimmtDieBuchungsreferenzMitLaufnummerWennDieTransaktionKeineHat() {
        var ohneTransaktionsreferenz = text("sammelbuchung.xml")
                .replace("<AcctSvcrRef>SAMMEL-1</AcctSvcrRef>", "")
                .replace("<AcctSvcrRef>SAMMEL-3</AcctSvcrRef>", "");

        var inhalt = PARSER.lesen(ohneTransaktionsreferenz.getBytes(StandardCharsets.UTF_8));

        assertThat(inhalt.gutschriften())
                .extracting(g -> g.bankReferenz())
                .containsExactly("SAMMEL-NTRY-1/1", "SAMMEL-2", "SAMMEL-NTRY-1/3");
        assertThat(inhalt.konten()).isEqualTo(Set.of("CH4431999123000889012"));
    }

    private static byte[] datei(String name) {
        try (var strom = Camt054ParserTest.class.getResourceAsStream("/camt/" + name)) {
            return strom.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(String name) {
        return new String(datei(name), StandardCharsets.UTF_8);
    }
}
