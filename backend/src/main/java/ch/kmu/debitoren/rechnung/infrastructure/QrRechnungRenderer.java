package ch.kmu.debitoren.rechnung.infrastructure;

import ch.kmu.debitoren.rechnung.domain.Empfaenger;
import ch.kmu.debitoren.rechnung.domain.QrReferenz;
import ch.kmu.debitoren.rechnung.domain.RechnungFehlercode;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung;
import ch.kmu.debitoren.shared.domain.Adresse;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.codecrete.qrbill.canvas.PDFCanvas;
import net.codecrete.qrbill.canvas.PDFFontSettings;
import net.codecrete.qrbill.generator.Address;
import net.codecrete.qrbill.generator.Bill;
import net.codecrete.qrbill.generator.GraphicsFormat;
import net.codecrete.qrbill.generator.Language;
import net.codecrete.qrbill.generator.OutputSize;
import net.codecrete.qrbill.generator.QRBill;
import net.codecrete.qrbill.generator.QRBillValidationError;
import net.codecrete.qrbill.generator.SwicoBillInformation;
import net.codecrete.qrbill.generator.ValidationMessage;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Erzeugt das Rechnungs-PDF: die Rechnung aus einer Thymeleaf-Vorlage mit OpenHTMLtoPDF, darauf unten auf
 * der letzten Seite den QR-Zahlteil von qrbill-generator. Der Zahlteil folgt den Swiss Payment Standards.
 */
@Component
class QrRechnungRenderer implements Rechnungsdarstellung {

    private static final Locale SCHWEIZ = Locale.forLanguageTag("de-CH");
    private static final int HOECHSTLAENGE_MITTEILUNGEN = 140;

    private static final Map<String, String> FELDER = Map.ofEntries(
            Map.entry("account", "Konto"),
            Map.entry("amount", "Betrag"),
            Map.entry("currency", "Währung"),
            Map.entry("reference", "Referenz"),
            Map.entry("unstructuredMessage", "Mitteilung"),
            Map.entry("billInformation", "Rechnungsinformationen"),
            Map.entry("creditor.name", "Name des Zahlungsempfängers"),
            Map.entry("creditor.postalCode", "PLZ des Zahlungsempfängers"),
            Map.entry("creditor.town", "Ort des Zahlungsempfängers"),
            Map.entry("creditor.countryCode", "Land des Zahlungsempfängers"),
            Map.entry("debtor.name", "Name des Zahlers"),
            Map.entry("debtor.postalCode", "PLZ des Zahlers"),
            Map.entry("debtor.town", "Ort des Zahlers"),
            Map.entry("debtor.countryCode", "Land des Zahlers"));

    private final ITemplateEngine vorlagen;

    QrRechnungRenderer(ITemplateEngine vorlagen) {
        this.vorlagen = vorlagen;
    }

    @Override
    public byte[] pdf(Rechnungsdokument dokument) {
        var kontext = new Context(SCHWEIZ);
        kontext.setVariable("r", RechnungAnsicht.aus(dokument));
        return mitZahlteil(htmlZuPdf(vorlagen.process("dokumente/rechnung", kontext)), dokument.zahlteil());
    }

    @Override
    public byte[] mahnungPdf(Mahndokument dokument) {
        var kontext = new Context(SCHWEIZ);
        kontext.setVariable("m", MahnschreibenAnsicht.aus(dokument));
        return mitZahlteil(htmlZuPdf(vorlagen.process("dokumente/mahnung", kontext)), dokument.zahlteil());
    }

    /** Setzt den QR-Zahlteil unten auf die letzte Seite; die Vorlagen lassen dort 112 mm frei. */
    private static byte[] mitZahlteil(byte[] dokument, Zahlteil zahlteil) {
        var bill = bill(zahlteil);
        bill.getFormat().setOutputSize(OutputSize.A4_PORTRAIT_SHEET);
        try (var seite = new PDFCanvas(dokument, PDFCanvas.LAST_PAGE, PDFFontSettings.embeddedLiberationSans())) {
            QRBill.draw(bill, seite);
            return seite.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (QRBillValidationError e) {
            throw ungueltig(e);
        }
    }

    @Override
    public String zahlteilSvg(Zahlteil zahlteil) {
        var bill = bill(zahlteil);
        bill.getFormat().setOutputSize(OutputSize.QR_BILL_ONLY);
        bill.getFormat().setGraphicsFormat(GraphicsFormat.SVG);
        try {
            return new String(QRBill.generate(bill), StandardCharsets.UTF_8);
        } catch (QRBillValidationError e) {
            throw ungueltig(e);
        }
    }

    @Override
    public List<String> zahlteilPruefen(Zahlteil zahlteil) {
        return fehler(QRBill.validate(bill(zahlteil)).getValidationMessages());
    }

    static Bill bill(Zahlteil zahlteil) {
        var bill = new Bill();
        bill.setAccount(zahlteil.empfaenger().konto().wert());
        bill.setCreditor(
                adresse(zahlteil.empfaenger().name(), zahlteil.empfaenger().adresse()));
        bill.setCurrency(zahlteil.waehrung().getCurrencyCode());
        if (zahlteil.betrag() != null) {
            bill.setAmount(zahlteil.betrag().betrag());
        }
        if (zahlteil.zahler() != null) {
            bill.setDebtor(adresse(zahlteil.zahler()));
        }
        if (zahlteil.referenz() == null) {
            bill.setReferenceType(Bill.REFERENCE_TYPE_NO_REF);
        } else {
            bill.setReferenceType(
                    zahlteil.referenz() instanceof QrReferenz
                            ? Bill.REFERENCE_TYPE_QR_REF
                            : Bill.REFERENCE_TYPE_CRED_REF);
            bill.setReference(zahlteil.referenz().wert());
        }

        var mitteilung = zahlteil.mitteilung();
        if (zahlteil.rechnungsangaben() != null) {
            var swico = swico(zahlteil.rechnungsangaben());
            bill.setBillInformation(swico);
            // Beide Felder teilen sich 140 Zeichen; die Swico-Angaben enthalten die Rechnungsnummer ohnehin.
            if (mitteilung != null && mitteilung.length() + swico.length() > HOECHSTLAENGE_MITTEILUNGEN) {
                mitteilung = null;
            }
        }
        bill.setUnstructuredMessage(mitteilung);
        bill.getFormat().setLanguage(Language.DE);
        return bill;
    }

    private static String swico(Rechnungsangaben angaben) {
        var swico = new SwicoBillInformation();
        swico.setInvoiceNumber(angaben.rechnungsnummer());
        swico.setInvoiceDate(angaben.rechnungsdatum());
        swico.setCustomerReference(angaben.kundennummer());
        if (angaben.mwstNummer() != null) {
            swico.setVatNumber(angaben.mwstNummer().replaceAll("\\D", ""));
            swico.setVatDate(angaben.rechnungsdatum());
            swico.setVatRateDetails(angaben.mwst().stream()
                    .map(anteil -> new SwicoBillInformation.RateDetail(
                            anteil.prozent(), anteil.basis().betrag()))
                    .toList());
        }
        swico.setPaymentConditions(
                List.of(new SwicoBillInformation.PaymentCondition(BigDecimal.ZERO, angaben.zahlungsfristTage())));
        return swico.encodeAsText();
    }

    private static Address adresse(Empfaenger empfaenger) {
        return adresse(empfaenger.name(), empfaenger.adresse());
    }

    private static Address adresse(String name, Adresse adresse) {
        var ergebnis = new Address();
        ergebnis.setName(name);
        ergebnis.setStreet(adresse.strasse());
        ergebnis.setHouseNo(adresse.hausnummer());
        ergebnis.setPostalCode(adresse.plz());
        ergebnis.setTown(adresse.ort());
        ergebnis.setCountryCode(adresse.land());
        return ergebnis;
    }

    private static byte[] htmlZuPdf(String html) {
        var ausgabe = new ByteArrayOutputStream();
        try {
            new PdfRendererBuilder()
                    .useFastMode()
                    .withHtmlContent(html, null)
                    .toStream(ausgabe)
                    .run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ausgabe.toByteArray();
    }

    private static FachlicherFehler ungueltig(QRBillValidationError fehler) {
        return new FachlicherFehler(
                RechnungFehlercode.ZAHLTEIL_UNGUELTIG,
                "Der QR-Zahlteil ist ungültig: "
                        + String.join("; ", fehler(fehler.getValidationResult().getValidationMessages())));
    }

    private static List<String> fehler(List<ValidationMessage> meldungen) {
        return meldungen.stream()
                .filter(meldung -> meldung.getType() == ValidationMessage.Type.ERROR)
                .map(meldung -> FELDER.getOrDefault(meldung.getField(), meldung.getField()) + " ("
                        + meldung.getMessageKey() + ")")
                .toList();
    }
}
