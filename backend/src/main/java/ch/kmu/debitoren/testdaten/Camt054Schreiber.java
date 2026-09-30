package ch.kmu.debitoren.testdaten;

import ch.kmu.debitoren.shared.domain.Geld;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Schreibt camt.054.001.08-Dateien für Testdaten und Demos: je Gutschrift eine Buchung mit einer Transaktion,
 * so wie Schweizer Banken QR-Zahlungen melden. Bewusst unabhängig vom Parser des Zahlungsimports, damit der
 * Import echte Dateien liest und nicht sein eigenes Spiegelbild.
 */
final class Camt054Schreiber {

    private static final String NAMENSRAUM = "urn:iso:std:iso:20022:tech:xsd:camt.054.001.08";
    private static final DateTimeFormatter ZEITPUNKT =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/Zurich"));

    /**
     * Eine Gutschrift in der Testdatei.
     *
     * @param referenz QR-Referenz, SCOR-Referenz ({@code RF…}) oder {@code null} für eine Zahlung ohne Referenz
     */
    record Testgutschrift(
            String bankReferenz,
            LocalDate valuta,
            Geld betrag,
            String referenz,
            String auftraggeber,
            String mitteilung) {}

    private Camt054Schreiber() {}

    static byte[] schreiben(String nachrichtId, Instant erstellt, String konto, List<Testgutschrift> gutschriften) {
        var ausgabe = new ByteArrayOutputStream();
        try {
            var xml = new Einrueckung(XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(ausgabe, "UTF-8"));
            xml.dokumentBeginnen();
            xml.oeffnen("Document");
            xml.namensraum(NAMENSRAUM);
            xml.oeffnen("BkToCstmrDbtCdtNtfctn");
            xml.oeffnen("GrpHdr");
            xml.element("MsgId", nachrichtId);
            xml.element("CreDtTm", ZEITPUNKT.format(erstellt));
            xml.schliessen();
            xml.oeffnen("Ntfctn");
            xml.element("Id", "NTF-" + nachrichtId);
            xml.element("CreDtTm", ZEITPUNKT.format(erstellt));
            xml.oeffnen("Acct");
            xml.oeffnen("Id");
            xml.element("IBAN", konto);
            xml.schliessen();
            xml.schliessen();
            for (var gutschrift : gutschriften) {
                buchung(xml, gutschrift);
            }
            xml.schliessen();
            xml.schliessen();
            xml.schliessen();
            xml.dokumentBeenden();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("camt.054-Testdatei nicht schreibbar", e);
        }
        return ausgabe.toByteArray();
    }

    private static void buchung(Einrueckung xml, Testgutschrift gutschrift) throws XMLStreamException {
        var betrag = gutschrift.betrag().betrag().toPlainString();
        var waehrung = gutschrift.betrag().waehrung().getCurrencyCode();
        xml.oeffnen("Ntry");
        xml.betrag("Amt", betrag, waehrung);
        xml.element("CdtDbtInd", "CRDT");
        xml.oeffnen("Sts");
        xml.element("Cd", "BOOK");
        xml.schliessen();
        xml.oeffnen("BookgDt");
        xml.element("Dt", gutschrift.valuta().toString());
        xml.schliessen();
        xml.oeffnen("ValDt");
        xml.element("Dt", gutschrift.valuta().toString());
        xml.schliessen();
        xml.element("AcctSvcrRef", gutschrift.bankReferenz() + "-B");
        xml.oeffnen("BkTxCd");
        xml.oeffnen("Domn");
        xml.element("Cd", "PMNT");
        xml.oeffnen("Fmly");
        xml.element("Cd", "RCDT");
        xml.element("SubFmlyCd", "VCOM");
        xml.schliessen();
        xml.schliessen();
        xml.schliessen();
        xml.oeffnen("NtryDtls");
        xml.oeffnen("TxDtls");
        xml.oeffnen("Refs");
        xml.element("AcctSvcrRef", gutschrift.bankReferenz());
        xml.element("EndToEndId", "NOTPROVIDED");
        xml.schliessen();
        xml.betrag("Amt", betrag, waehrung);
        xml.element("CdtDbtInd", "CRDT");
        if (gutschrift.auftraggeber() != null) {
            xml.oeffnen("RltdPties");
            xml.oeffnen("Dbtr");
            xml.oeffnen("Pty");
            xml.element("Nm", gutschrift.auftraggeber());
            xml.schliessen();
            xml.schliessen();
            xml.schliessen();
        }
        xml.oeffnen("RmtInf");
        if (gutschrift.referenz() == null) {
            xml.element("Ustrd", gutschrift.mitteilung() == null ? "Zahlung" : gutschrift.mitteilung());
        } else {
            xml.oeffnen("Strd");
            xml.oeffnen("CdtrRefInf");
            xml.oeffnen("Tp");
            xml.oeffnen("CdOrPrtry");
            if (gutschrift.referenz().startsWith("RF")) {
                xml.element("Cd", "SCOR");
            } else {
                xml.element("Prtry", "QRR");
            }
            xml.schliessen();
            xml.schliessen();
            xml.element("Ref", gutschrift.referenz());
            xml.schliessen();
            xml.schliessen();
        }
        xml.schliessen();
        xml.schliessen();
        xml.schliessen();
        xml.schliessen();
    }

    /** Schreibt eingerückt, damit Menschen die Testdateien lesen können. */
    private static final class Einrueckung {

        private final XMLStreamWriter xml;
        private int tiefe;

        Einrueckung(XMLStreamWriter xml) {
            this.xml = xml;
        }

        void dokumentBeginnen() throws XMLStreamException {
            xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
        }

        void namensraum(String namensraum) throws XMLStreamException {
            xml.writeDefaultNamespace(namensraum);
        }

        void oeffnen(String name) throws XMLStreamException {
            zeile();
            xml.writeStartElement(name);
            tiefe++;
        }

        void schliessen() throws XMLStreamException {
            tiefe--;
            zeile();
            xml.writeEndElement();
        }

        void element(String name, String text) throws XMLStreamException {
            zeile();
            xml.writeStartElement(name);
            xml.writeCharacters(text);
            xml.writeEndElement();
        }

        void betrag(String name, String betrag, String waehrung) throws XMLStreamException {
            zeile();
            xml.writeStartElement(name);
            xml.writeAttribute("Ccy", waehrung);
            xml.writeCharacters(betrag);
            xml.writeEndElement();
        }

        void dokumentBeenden() throws XMLStreamException {
            xml.writeCharacters("\n");
            xml.writeEndDocument();
            xml.close();
        }

        private void zeile() throws XMLStreamException {
            xml.writeCharacters("\n" + "  ".repeat(tiefe));
        }
    }
}
