package ch.kmu.debitoren.zahlungsimport.infrastructure;

import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.zahlungsimport.domain.Gutschrift;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiParser;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsdateiinhalt;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsimportFehlercode;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.AccountIdentification4Choice;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.AccountNotification17;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.ActiveOrHistoricCurrencyAndAmount;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.CreditDebitCode;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.CreditorReferenceInformation2;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.DateAndDateTime2Choice;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.Document;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.DocumentType3Code;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.EntryDetails9;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.EntryTransaction10;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.Party40Choice;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.RemittanceInformation16;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.ReportEntry10;
import ch.kmu.debitoren.zahlungsimport.infrastructure.camt054.TransactionParties6;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.ValidationEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * Liest camt.054.001.08 (Swiss Payment Standards ab 2022). Die Datei wird ohne DTD und externe Entitäten
 * gelesen (Schutz vor XXE) und beim Einlesen gegen das XSD geprüft. Übernommen werden gebuchte
 * Gutschriften; jede Transaktion einer Sammelbuchung wird eine eigene Gutschrift.
 */
@Component
class Camt054Parser implements ZahlungsdateiParser {

    static final String NAMENSRAUM = "urn:iso:std:iso:20022:tech:xsd:camt.054.001.08";

    private static final String SCHEMA = "/camt/camt.054.001.08.xsd";
    private static final ZoneId SCHWEIZ = ZoneId.of("Europe/Zurich");
    private static final int HOECHSTENS_GEMELDETE_FEHLER = 5;
    private static final BigDecimal HOECHSTBETRAG = new BigDecimal("9999999999.99");

    private final JAXBContext kontext;
    private final Schema schema;
    private final XMLInputFactory eingabeFabrik;

    Camt054Parser() {
        try {
            this.kontext = JAXBContext.newInstance(Document.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("JAXB-Kontext für camt.054 nicht erstellbar", e);
        }
        this.schema = schemaLaden();
        this.eingabeFabrik = sichereEingabeFabrik();
    }

    @Override
    public Zahlungsdateiinhalt lesen(byte[] inhalt) {
        if (inhalt == null || inhalt.length == 0) {
            throw ungueltig("Die Datei ist leer.");
        }
        var dokument = einlesen(inhalt);
        return umwandeln(dokument);
    }

    private Document einlesen(byte[] inhalt) {
        XMLStreamReader leser = null;
        try {
            leser = eingabeFabrik.createXMLStreamReader(new ByteArrayInputStream(inhalt));
            while (leser.hasNext() && leser.getEventType() != XMLStreamConstants.START_ELEMENT) {
                leser.next();
            }
            if (leser.getEventType() != XMLStreamConstants.START_ELEMENT
                    || !"Document".equals(leser.getLocalName())
                    || !NAMENSRAUM.equals(leser.getNamespaceURI())) {
                throw ungueltig("Die Datei ist kein camt.054-Dokument der Version 001.08.");
            }
            var fehler = new ArrayList<String>();
            var unmarshaller = kontext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.setEventHandler(ereignis -> {
                fehler.add(meldung(ereignis));
                return ereignis.getSeverity() != ValidationEvent.FATAL_ERROR
                        && fehler.size() < HOECHSTENS_GEMELDETE_FEHLER;
            });
            Document dokument;
            try {
                dokument = unmarshaller.unmarshal(leser, Document.class).getValue();
            } catch (JAXBException e) {
                if (fehler.isEmpty()) {
                    throw e;
                }
                dokument = null;
            }
            if (!fehler.isEmpty() || dokument == null) {
                throw ungueltig("Die Datei entspricht nicht dem Schema camt.054.001.08: " + String.join("; ", fehler));
            }
            return dokument;
        } catch (XMLStreamException | JAXBException e) {
            throw ungueltig("Die Datei ist kein gültiges XML: " + ursache(e));
        } finally {
            schliessen(leser);
        }
    }

    private static Zahlungsdateiinhalt umwandeln(Document dokument) {
        var nachricht = dokument.getBkToCstmrDbtCdtNtfctn();
        var kopf = nachricht.getGrpHdr();
        var konten = new LinkedHashSet<String>();
        var gutschriften = new ArrayList<Gutschrift>();
        int ignoriert = 0;
        for (AccountNotification17 meldung : nachricht.getNtfctn()) {
            konten.add(konto(meldung.getAcct().getId()));
            for (ReportEntry10 buchung : meldung.getNtry()) {
                var transaktionen = buchung.getNtryDtls().stream()
                        .map(EntryDetails9::getTxDtls)
                        .flatMap(List::stream)
                        .toList();
                if (!istGebuchteGutschrift(buchung)) {
                    ignoriert += Math.max(1, transaktionen.size());
                    continue;
                }
                if (transaktionen.isEmpty()) {
                    gutschriften.add(ohneDetails(buchung));
                    continue;
                }
                for (int i = 0; i < transaktionen.size(); i++) {
                    var transaktion = transaktionen.get(i);
                    if (transaktion.getCdtDbtInd() == CreditDebitCode.DBIT) {
                        ignoriert++;
                        continue;
                    }
                    gutschriften.add(gutschrift(buchung, transaktion, i, transaktionen.size()));
                }
            }
        }
        return new Zahlungsdateiinhalt(kopf.getMsgId(), zeitpunkt(kopf.getCreDtTm()), konten, gutschriften, ignoriert);
    }

    /** Nur gebuchte Gutschriften; Belastungen, Stornos und Vormerkungen zählen als ignoriert. */
    private static boolean istGebuchteGutschrift(ReportEntry10 buchung) {
        return buchung.getCdtDbtInd() == CreditDebitCode.CRDT
                && !Boolean.TRUE.equals(buchung.isRvslInd())
                && buchung.getSts() != null
                && "BOOK".equals(buchung.getSts().getCd());
    }

    private static Gutschrift ohneDetails(ReportEntry10 buchung) {
        return new Gutschrift(
                Optional.ofNullable(buchung.getAcctSvcrRef())
                        .orElseThrow(() -> ungueltig("Eine Buchung hat keine Bankreferenz (AcctSvcrRef).")),
                datum(buchung.getBookgDt()),
                valuta(buchung),
                betrag(buchung.getAmt()),
                null,
                null,
                null,
                buchung.getAddtlNtryInf());
    }

    private static Gutschrift gutschrift(ReportEntry10 buchung, EntryTransaction10 transaktion, int index, int anzahl) {
        var parteien = transaktion.getRltdPties();
        return new Gutschrift(
                bankReferenz(buchung, transaktion, index, anzahl),
                datum(buchung.getBookgDt()),
                valuta(buchung),
                betrag(transaktionsbetrag(buchung, transaktion, anzahl)),
                referenz(transaktion.getRmtInf()),
                auftraggeber(parteien),
                auftraggeberIban(parteien),
                mitteilung(transaktion.getRmtInf(), transaktion.getAddtlTxInf()));
    }

    /**
     * Die Referenz der Bank für die einzelne Transaktion, sonst die der Buchung mit laufender Nummer. Ohne
     * beides liessen sich Buchungen, die in mehreren Dateien vorkommen, nicht erkennen.
     */
    private static String bankReferenz(ReportEntry10 buchung, EntryTransaction10 transaktion, int index, int anzahl) {
        var refs = transaktion.getRefs();
        if (refs != null && istGesetzt(refs.getAcctSvcrRef())) {
            return refs.getAcctSvcrRef().strip();
        }
        if (istGesetzt(buchung.getAcctSvcrRef())) {
            var basis = buchung.getAcctSvcrRef().strip();
            return anzahl == 1 ? basis : basis + "/" + (index + 1);
        }
        throw ungueltig("Eine Buchung hat keine Bankreferenz (AcctSvcrRef).");
    }

    private static ActiveOrHistoricCurrencyAndAmount transaktionsbetrag(
            ReportEntry10 buchung, EntryTransaction10 transaktion, int anzahl) {
        if (transaktion.getAmt() != null) {
            return transaktion.getAmt();
        }
        var details = transaktion.getAmtDtls();
        if (details != null && details.getTxAmt() != null) {
            return details.getTxAmt().getAmt();
        }
        if (anzahl == 1) {
            return buchung.getAmt();
        }
        throw ungueltig("Eine Transaktion einer Sammelbuchung hat keinen Betrag.");
    }

    private static Geld betrag(ActiveOrHistoricCurrencyAndAmount betrag) {
        var wert = betrag.getValue();
        if (wert.stripTrailingZeros().scale() > 2 || wert.compareTo(HOECHSTBETRAG) > 0) {
            throw ungueltig("Der Betrag %s ist kein gültiger Betrag mit höchstens zwei Nachkommastellen."
                    .formatted(wert.toPlainString()));
        }
        try {
            return new Geld(wert, Currency.getInstance(betrag.getCcy()));
        } catch (IllegalArgumentException e) {
            throw ungueltig("Unbekannte Währung " + betrag.getCcy());
        }
    }

    /** QR-Referenz ({@code QRR}) oder Creditor Reference ({@code SCOR}); unbekannte Arten fallen weg. */
    private static String referenz(RemittanceInformation16 mitteilung) {
        if (mitteilung == null) {
            return null;
        }
        return mitteilung.getStrd().stream()
                .map(strukturiert -> strukturiert.getCdtrRefInf())
                .filter(Objects::nonNull)
                .filter(Camt054Parser::istZahlungsreferenz)
                .map(CreditorReferenceInformation2::getRef)
                .filter(Camt054Parser::istGesetzt)
                .findFirst()
                .orElse(null);
    }

    private static boolean istZahlungsreferenz(CreditorReferenceInformation2 info) {
        var art = info.getTp() == null ? null : info.getTp().getCdOrPrtry();
        if (art == null) {
            return true;
        }
        return art.getCd() == DocumentType3Code.SCOR
                || (art.getPrtry() != null
                        && "QRR".equalsIgnoreCase(art.getPrtry().strip()));
    }

    private static String auftraggeber(TransactionParties6 parteien) {
        if (parteien == null) {
            return null;
        }
        return Stream.of(parteien.getDbtr(), parteien.getUltmtDbtr())
                .map(Camt054Parser::name)
                .filter(Camt054Parser::istGesetzt)
                .findFirst()
                .orElse(null);
    }

    private static String name(Party40Choice partei) {
        return partei == null || partei.getPty() == null
                ? null
                : partei.getPty().getNm();
    }

    private static String auftraggeberIban(TransactionParties6 parteien) {
        if (parteien == null
                || parteien.getDbtrAcct() == null
                || parteien.getDbtrAcct().getId() == null) {
            return null;
        }
        return parteien.getDbtrAcct().getId().getIBAN();
    }

    private static String mitteilung(RemittanceInformation16 mitteilung, String zusatz) {
        var teile = new ArrayList<String>();
        if (mitteilung != null) {
            teile.addAll(mitteilung.getUstrd());
            mitteilung.getStrd().forEach(strukturiert -> teile.addAll(strukturiert.getAddtlRmtInf()));
        }
        if (zusatz != null) {
            teile.add(zusatz);
        }
        var text = teile.stream()
                .filter(Camt054Parser::istGesetzt)
                .map(String::strip)
                .collect(Collectors.joining(" "));
        return text.isEmpty() ? null : text;
    }

    private static String konto(AccountIdentification4Choice id) {
        if (id.getIBAN() != null) {
            return id.getIBAN().replace(" ", "").toUpperCase(Locale.ROOT);
        }
        return id.getOthr() == null ? "" : id.getOthr().getId().strip();
    }

    private static LocalDate valuta(ReportEntry10 buchung) {
        return Optional.ofNullable(datum(buchung.getValDt()))
                .or(() -> Optional.ofNullable(datum(buchung.getBookgDt())))
                .orElseThrow(() -> ungueltig("Eine Buchung hat weder Valuta noch Buchungsdatum."));
    }

    private static LocalDate datum(DateAndDateTime2Choice wahl) {
        if (wahl == null) {
            return null;
        }
        if (wahl.getDt() != null) {
            var dt = wahl.getDt();
            return LocalDate.of(dt.getYear(), dt.getMonth(), dt.getDay());
        }
        return wahl.getDtTm() == null
                ? null
                : zeitpunkt(wahl.getDtTm()).atZone(SCHWEIZ).toLocalDate();
    }

    /** Zeitpunkte ohne Zeitzone gelten als Schweizer Zeit. */
    private static Instant zeitpunkt(XMLGregorianCalendar wert) {
        var zone = wert.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? TimeZone.getTimeZone(SCHWEIZ) : null;
        return wert.toGregorianCalendar(zone, Locale.ROOT, null).toInstant();
    }

    private static boolean istGesetzt(String text) {
        return text != null && !text.isBlank() && !"NOTPROVIDED".equalsIgnoreCase(text.strip());
    }

    private static String meldung(ValidationEvent ereignis) {
        var ort = ereignis.getLocator();
        var zeile = ort == null || ort.getLineNumber() < 0 ? "" : "Zeile " + ort.getLineNumber() + ": ";
        return zeile + ereignis.getMessage();
    }

    private static String ursache(Exception e) {
        Throwable ursache = e;
        while (ursache.getCause() != null && ursache.getMessage() == null) {
            ursache = ursache.getCause();
        }
        if (e instanceof JAXBException jaxb && jaxb.getLinkedException() != null) {
            ursache = jaxb.getLinkedException();
        }
        return String.valueOf(ursache.getMessage());
    }

    private static FachlicherFehler ungueltig(String meldung) {
        return new FachlicherFehler(ZahlungsimportFehlercode.DATEI_UNGUELTIG, meldung);
    }

    private static void schliessen(XMLStreamReader leser) {
        if (leser != null) {
            try {
                leser.close();
            } catch (XMLStreamException e) {
                // Das Lesen ist abgeschlossen; ein Fehler beim Schliessen ändert am Ergebnis nichts.
            }
        }
    }

    private static XMLInputFactory sichereEingabeFabrik() {
        var fabrik = XMLInputFactory.newDefaultFactory();
        fabrik.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrik.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        fabrik.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        return fabrik;
    }

    private static Schema schemaLaden() {
        try (var xsd = Camt054Parser.class.getResourceAsStream(SCHEMA)) {
            if (xsd == null) {
                throw new IllegalStateException("Schema fehlt im Klassenpfad: " + SCHEMA);
            }
            var fabrik = SchemaFactory.newDefaultInstance();
            fabrik.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrik.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return fabrik.newSchema(new StreamSource(xsd));
        } catch (SAXException e) {
            throw new IllegalStateException("Schema camt.054.001.08 ist ungültig", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
