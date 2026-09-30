package ch.kmu.debitoren.rechnung.infrastructure;

import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.NORMAL_2024;
import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.REDUZIERT_2024;
import static ch.kmu.debitoren.rechnung.domain.RechnungTestBuilder.rechnung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.kmu.debitoren.rechnung.domain.Mahnangaben;
import ch.kmu.debitoren.rechnung.domain.QrReferenz;
import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Mahndokument;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungsangaben;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungsdokument;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungssteller;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Zahlteil;
import ch.kmu.debitoren.rechnung.domain.ScorReferenz;
import ch.kmu.debitoren.rechnung.domain.Zahlungsreferenz;
import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import net.codecrete.qrbill.generator.QRBill;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * PDF und Zahlteil ohne Spring. Die erzeugten PDFs liegen danach unter target/qr-zahlteil-beispiele und
 * lassen sich für Gate 2 im Validierungsportal von SIX prüfen.
 */
class QrRechnungRendererTest {

    private static final Path BEISPIELE = Path.of("target", "qr-zahlteil-beispiele");

    private static final Rechnungssteller MIT_QR_IBAN = new Rechnungssteller(
            "Berner Werkstatt AG",
            Adresse.schweiz("Marktgasse", "28", "3011", "Bern"),
            new Iban("CH4431999123000889012"),
            "CHE-100.200.300 MWST",
            "rechnungen@berner-werkstatt.example");

    private static final Rechnungssteller MIT_IBAN = new Rechnungssteller(
            "Seeland Gartenbau GmbH",
            Adresse.schweiz("Hauptstrasse", "5", "2560", "Nidau"),
            new Iban("CH9300762011623852957"),
            null,
            null);

    private static QrRechnungRenderer renderer;

    @BeforeAll
    static void vorlagenLaden() throws IOException {
        var aufloeser = new ClassLoaderTemplateResolver();
        aufloeser.setPrefix("templates/");
        aufloeser.setSuffix(".html");
        aufloeser.setTemplateMode(TemplateMode.HTML);
        aufloeser.setCharacterEncoding("UTF-8");
        var vorlagen = new SpringTemplateEngine();
        vorlagen.setTemplateResolver(aufloeser);
        renderer = new QrRechnungRenderer(vorlagen);
        Files.createDirectories(BEISPIELE);
    }

    @Test
    void pdfEnthaeltRechnungUndZahlteilAufEinerSeite() throws IOException {
        var rechnung = freigegeben("RE-2026-0412", QrReferenz.aus("2026000412"));
        var pdf = renderer.pdf(dokument(MIT_QR_IBAN, rechnung));
        Files.write(BEISPIELE.resolve("qr-referenz-chf.pdf"), pdf);

        try (var dokument = Loader.loadPDF(pdf)) {
            assertThat(dokument.getNumberOfPages()).isEqualTo(1);
            var text = new PDFTextStripper().getText(dokument);
            assertThat(text)
                    .contains("Rechnung RE-2026-0412")
                    .contains("Berner Werkstatt AG")
                    .contains("MWST-Nr. CHE-100.200.300 MWST")
                    .contains("Keller Haustechnik AG")
                    .contains("Kundennummer")
                    .contains("K-10001")
                    .contains("Montage vor Ort")
                    .contains("MWST 8.1 % auf 360.00")
                    .contains("Total CHF")
                    .contains("464.55")
                    .contains(rechnung.getReferenz().formatiert())
                    .contains("Zahlteil")
                    .contains("Empfangsschein");
        }
    }

    @Test
    void qrCodeEnthaeltReferenzBetragUndSwicoAngaben() {
        var rechnung = freigegeben("RE-2026-0412", QrReferenz.aus("2026000412"));
        var text = QRBill.encodeQrCodeText(QrRechnungRenderer.bill(zahlteil(MIT_QR_IBAN, rechnung)));

        assertThat(text)
                .startsWith("SPC")
                .contains("CH4431999123000889012")
                .contains("464.55")
                .contains("CHF")
                .contains("QRR")
                .contains(rechnung.getReferenz().wert())
                .contains("//S1/10/RE-2026-0412/11/260901/20/K-10001/30/100200300");
    }

    @Test
    void scorReferenzFuerMandantenOhneQrIban() throws IOException {
        var rechnung = freigegeben("RE-2026-0413", ScorReferenz.aus("2026000413"));
        var pdf = renderer.pdf(dokument(MIT_IBAN, rechnung));
        Files.write(BEISPIELE.resolve("scor-referenz-chf.pdf"), pdf);

        var text = QRBill.encodeQrCodeText(QrRechnungRenderer.bill(zahlteil(MIT_IBAN, rechnung)));
        assertThat(text).contains("SCOR").contains(rechnung.getReferenz().wert());
        assertThat(renderer.zahlteilPruefen(zahlteil(MIT_IBAN, rechnung))).isEmpty();
    }

    @Test
    void euroRechnung() throws IOException {
        var rechnung = rechnung()
                .waehrung(Geld.EUR)
                .position("Service Grenzregion", "4", "95.00", NORMAL_2024)
                .entwurf();
        rechnung.freigeben(
                "RE-2026-0414",
                QrReferenz.aus("2026000414"),
                rechnung().empfaenger(),
                Instant.parse("2026-09-01T08:00:00Z"));
        var pdf = renderer.pdf(dokument(MIT_QR_IBAN, rechnung));
        Files.write(BEISPIELE.resolve("qr-referenz-eur.pdf"), pdf);

        assertThat(QRBill.encodeQrCodeText(QrRechnungRenderer.bill(zahlteil(MIT_QR_IBAN, rechnung))))
                .contains("EUR")
                .contains("410.78");
    }

    @Test
    void mahnschreibenMitGebuehrUndZahlteilUeberDenOffenenBetrag() throws IOException {
        var rechnung = freigegeben("RE-2026-0412", QrReferenz.aus("2026000412"));
        var angaben = new Mahnangaben(
                "2. Mahnung",
                LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 11, 9),
                "Leider ist die Rechnung noch offen.\n\nFür den Aufwand verrechnen wir eine Gebühr.",
                rechnung.getTotal(),
                Geld.chf("100.00"),
                Geld.chf("20.00"),
                Geld.chf("384.55"));
        var zahlteil = new Zahlteil(
                MIT_QR_IBAN,
                Geld.CHF,
                angaben.offen(),
                rechnung.getEmpfaenger(),
                rechnung.getReferenz(),
                "2. Mahnung zu Rechnung RE-2026-0412",
                null);
        var pdf = renderer.mahnungPdf(
                new Mahndokument(MIT_QR_IBAN, rechnung, "K-10001", rechnung.getEmpfaenger(), angaben, zahlteil));
        Files.write(BEISPIELE.resolve("mahnung-qr-referenz-chf.pdf"), pdf);

        try (var dokument = Loader.loadPDF(pdf)) {
            assertThat(dokument.getNumberOfPages()).isEqualTo(1);
            var text = new PDFTextStripper().getText(dokument);
            assertThat(text)
                    .contains("2. Mahnung")
                    .contains("Bern, 30.10.2026")
                    .contains("Rechnung RE-2026-0412")
                    .contains("Fällig seit")
                    .contains("Leider ist die Rechnung noch offen.")
                    .contains("Für den Aufwand verrechnen wir eine Gebühr.")
                    .contains("Rechnungsbetrag")
                    .contains("464.55")
                    .contains("Bereits bezahlt")
                    .contains("-100.00")
                    .contains("Mahngebühren")
                    .contains("Offener Betrag CHF")
                    .contains("384.55")
                    .contains("bis am 09.11.2026")
                    .contains("Zahlteil");
        }
        assertThat(QRBill.encodeQrCodeText(QrRechnungRenderer.bill(zahlteil)))
                .contains("384.55")
                .contains(rechnung.getReferenz().wert())
                .contains("2. Mahnung zu Rechnung RE-2026-0412");
    }

    @Test
    void absaetzeTrennenSichAnLeerzeilen() {
        assertThat(MahnschreibenAnsicht.absaetze("Erster\nAbsatz.\n\n  Zweiter Absatz.  \n\n\n"))
                .containsExactly("Erster Absatz.", "Zweiter Absatz.");
        assertThat(MahnschreibenAnsicht.absaetze(" ")).isEmpty();
    }

    @Test
    void zahlteilAlsSvg() {
        var svg =
                renderer.zahlteilSvg(zahlteil(MIT_QR_IBAN, freigegeben("RE-2026-0412", QrReferenz.aus("2026000412"))));
        assertThat(svg).contains("<svg").contains("Zahlteil").contains("464.55");
    }

    @Test
    void vorschauOhneBetragUndOhneZahler() {
        var zahlteil = new Zahlteil(MIT_IBAN, Geld.CHF, null, null, null, "Vorschau", null);
        assertThat(renderer.zahlteilPruefen(zahlteil)).isEmpty();
        assertThat(renderer.zahlteilSvg(zahlteil)).contains("<svg");
    }

    @Test
    void qrIbanOhneQrReferenzIstUngueltig() {
        var rechnung = freigegeben("RE-2026-0415", ScorReferenz.aus("2026000415"));
        var zahlteil = zahlteil(MIT_QR_IBAN, rechnung);

        assertThat(renderer.zahlteilPruefen(zahlteil)).isNotEmpty();
        assertThatThrownBy(() -> renderer.zahlteilSvg(zahlteil))
                .isInstanceOfSatisfying(
                        FachlicherFehler.class,
                        fehler -> assertThat(fehler.code()).isEqualTo(RechnungFehlercode.ZAHLTEIL_UNGUELTIG));
    }

    @Test
    void langeRechnungBrichtAufMehrereSeitenUm() throws IOException {
        var lang = rechnung();
        for (int i = 1; i <= 40; i++) {
            lang.position("Arbeitsschritt " + i, "1", "10.00", NORMAL_2024);
        }
        var rechnung = lang.entwurf();
        rechnung.freigeben(
                "RE-2026-0416",
                QrReferenz.aus("2026000416"),
                rechnung().empfaenger(),
                Instant.parse("2026-09-01T08:00:00Z"));

        try (var dokument = Loader.loadPDF(renderer.pdf(dokument(MIT_QR_IBAN, rechnung)))) {
            assertThat(dokument.getNumberOfPages()).isGreaterThan(1);
            var stripper = new PDFTextStripper();
            stripper.setStartPage(dokument.getNumberOfPages());
            assertThat(stripper.getText(dokument)).contains("Zahlteil");
        }
    }

    private static Rechnung freigegeben(String nummer, Zahlungsreferenz referenz) {
        var rechnung = rechnung()
                .datum(LocalDate.of(2026, 9, 1))
                .bemerkung("Arbeiten gemäss Offerte vom 12.08.2026")
                .position("Montage vor Ort", "3", "120.00", NORMAL_2024)
                .position("Pflanzen", "10", "7.35", REDUZIERT_2024)
                .entwurf();
        rechnung.freigeben(nummer, referenz, rechnung().empfaenger(), Instant.parse("2026-09-01T08:00:00Z"));
        return rechnung;
    }

    private static Rechnungsdokument dokument(Rechnungssteller steller, Rechnung rechnung) {
        return new Rechnungsdokument(steller, rechnung, "K-10001", zahlteil(steller, rechnung));
    }

    private static Zahlteil zahlteil(Rechnungssteller steller, Rechnung rechnung) {
        return new Zahlteil(
                steller,
                rechnung.getWaehrung(),
                rechnung.getTotal(),
                rechnung.getEmpfaenger(),
                rechnung.getReferenz(),
                "Rechnung " + rechnung.getRechnungsnummer(),
                new Rechnungsangaben(
                        rechnung.getRechnungsnummer(),
                        rechnung.getRechnungsdatum(),
                        "K-10001",
                        steller.mwstNummer(),
                        rechnung.summen().mwst(),
                        rechnung.getZahlungsfristTage()));
    }
}
