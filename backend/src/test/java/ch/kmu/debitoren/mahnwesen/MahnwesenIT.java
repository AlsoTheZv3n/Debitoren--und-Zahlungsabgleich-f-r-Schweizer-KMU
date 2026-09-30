package ch.kmu.debitoren.mahnwesen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.kmu.debitoren.ApiTestBasis;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Mahnlauf, Freigabe, Versand per E-Mail und Post, Gebühren und Rollen, durch alle Schichten. */
class MahnwesenIT extends ApiTestBasis {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalDate HEUTE = LocalDate.now(ZoneId.of("Europe/Zurich"));

    @Autowired
    GreenMail mailserver;

    private UUID mandant;
    private String admin;
    private String buchhaltung;

    @BeforeEach
    void vorbereiten() {
        mandant = mandantMitQrIban("Mahnwesen Test AG");
        admin = token("admin", mandant);
        buchhaltung = token("buchhaltung", mandant);
    }

    @Test
    void mahnlaufSchlaegtVorUndDieFreigabeVersendetPerEmail() throws Exception {
        var email = "keller@" + mandant + ".example";
        var kunde = kundeAnlegen(buchhaltung, mandant, mitEmail("Keller Haustechnik AG", email));
        var rechnung = ueberfaelligeRechnung(kunde, "250.00", 35);

        var lauf = post(admin, "/api/mahnlaeufe", Map.of());
        assertThat(lauf.get("vorschlaege").asInt()).isEqualTo(1);
        assertThat(lauf.get("ausgeloestVon").asString()).isEqualTo("admin");

        var vorschlag = einzigerVorschlag();
        assertThat(vorschlag.get("rechnungId").asString())
                .isEqualTo(rechnung.get("id").asString());
        assertThat(vorschlag.get("stufe").asInt()).isEqualTo(1);
        assertThat(vorschlag.get("bezeichnung").asString()).isEqualTo("Zahlungserinnerung");
        assertThat(vorschlag.get("tageUeberfaellig").asInt()).isEqualTo(35);
        assertThat(vorschlag.get("zuZahlen").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(pdf(buchhaltung, vorschlag.get("id").asString()))
                .startsWith("%PDF".getBytes(StandardCharsets.US_ASCII));

        // Ein zweiter Lauf am selben Tag findet nichts Neues.
        assertThat(post(admin, "/api/mahnlaeufe", Map.of()).get("vorschlaege").asInt())
                .isZero();

        var ergebnis = freigeben(buchhaltung, vorschlag);
        assertThat(ergebnis.get("freigegeben").asInt()).isEqualTo(1);
        assertThat(ergebnis.get("nichtFreigegeben").size()).isZero();

        var versendet = aufStatusWarten(vorschlag.get("id").asString(), "VERSENDET");
        assertThat(versendet.get("versandweg").asString()).isEqualTo("EMAIL");
        assertThat(versendet.get("freigegebenVon").asString()).isEqualTo("buchhaltung");
        var mail = mailAn(email);
        assertThat(mail.getSubject())
                .isEqualTo("Zahlungserinnerung zu Rechnung "
                        + rechnung.get("rechnungsnummer").asString());
        assertThat(mail.getFrom()[0].toString()).contains("Mahnwesen Test AG");
        var teile = (Multipart) mail.getContent();
        assertThat(GreenMailUtil.getBody(teile.getBodyPart(0))).contains("CHF 250.00");
        assertThat(teile.getBodyPart(1).getContentType()).startsWith("application/pdf");
        assertThat(teile.getBodyPart(1).getFileName()).endsWith("-Zahlungserinnerung.pdf");

        var derRechnung = getListe(
                buchhaltung, "/api/mahnungen/rechnung/" + rechnung.get("id").asString());
        assertThat(derRechnung.size()).isEqualTo(1);
        assertThat(derRechnung.get(0).get("status").asString()).isEqualTo("VERSENDET");
    }

    @Test
    void gebuehrGehoertZurForderungBisSieBezahltIst() {
        stufenFestlegen(List.of(stufe("1. Mahnung", 10, "20.00")));
        var kunde = kundeAnlegen(buchhaltung, mandant, "Gasser Treuhand GmbH");
        var rechnung = ueberfaelligeRechnung(kunde, "100.00", 20);
        var id = rechnung.get("id").asString();

        post(admin, "/api/mahnlaeufe", Map.of());
        var vorschlag = einzigerVorschlag();
        assertThat(vorschlag.get("gebuehr").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(vorschlag.get("zuZahlen").decimalValue()).isEqualByComparingTo("120.00");
        freigeben(buchhaltung, vorschlag);

        var gemahnt = get(buchhaltung, "/api/rechnungen/" + id);
        assertThat(gemahnt.get("mahngebuehren").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(gemahnt.get("offen").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(gemahnt.get("status").asString()).isEqualTo("OFFEN");
        aufStatusWarten(vorschlag.get("id").asString(), "VERSENDET");

        // Wer nur die Rechnung bezahlt, schuldet noch die Gebühr.
        var referenz = rechnung.get("referenz").asString().replace(" ", "");
        importieren(camt("100.00", referenz));
        await().atMost(Duration.ofSeconds(30))
                .until(() -> "TEILBEZAHLT"
                        .equals(get(buchhaltung, "/api/rechnungen/" + id)
                                .get("status")
                                .asString()));
        assertThat(get(buchhaltung, "/api/rechnungen/" + id).get("offen").decimalValue())
                .isEqualByComparingTo("20.00");

        importieren(camt("20.00", referenz));
        await().atMost(Duration.ofSeconds(30))
                .until(() -> "BEZAHLT"
                        .equals(get(buchhaltung, "/api/rechnungen/" + id)
                                .get("status")
                                .asString()));
    }

    @Test
    void vorschlagZuStornierterRechnungWirdBeiDerFreigabeVerworfen() {
        var kunde = kundeAnlegen(buchhaltung, mandant, "Lüthi Bau AG");
        var rechnung = ueberfaelligeRechnung(kunde, "80.00", 15);
        post(admin, "/api/mahnlaeufe", Map.of());
        var vorschlag = einzigerVorschlag();
        post(
                buchhaltung,
                "/api/rechnungen/" + rechnung.get("id").asString() + "/storno",
                Map.of("version", rechnung.get("version").asLong(), "grund", "Doppelt erfasst"));

        var ergebnis = freigeben(buchhaltung, vorschlag);
        assertThat(ergebnis.get("freigegeben").asInt()).isZero();
        assertThat(ergebnis.get("nichtFreigegeben").get(0).get("grund").asString())
                .isEqualTo("Rechnung storniert; Vorschlag verworfen.");
        var verworfen = get(buchhaltung, "/api/mahnungen/" + vorschlag.get("id").asString());
        assertThat(verworfen.get("status").asString()).isEqualTo("VERWORFEN");
        assertThat(verworfen.get("verworfenGrund").asString()).isEqualTo("Rechnung storniert");
    }

    @Test
    void verworfenMitSperreKommtBisDahinNichtWieder() {
        var kunde = kundeAnlegen(buchhaltung, mandant, "Brunner Garten");
        ueberfaelligeRechnung(kunde, "95.50", 12);
        post(admin, "/api/mahnlaeufe", Map.of());
        var vorschlag = einzigerVorschlag();

        var verworfen = post(
                buchhaltung,
                "/api/mahnungen/" + vorschlag.get("id").asString() + "/verwerfen",
                Map.of(
                        "version",
                        vorschlag.get("version").asLong(),
                        "grund",
                        "Kunde hat Zahlung zugesagt",
                        "gesperrtBis",
                        HEUTE.plusDays(10).toString()));
        assertThat(verworfen.get("status").asString()).isEqualTo("VERWORFEN");
        assertThat(verworfen.get("verworfenVon").asString()).isEqualTo("buchhaltung");

        assertThat(post(admin, "/api/mahnlaeufe", Map.of()).get("vorschlaege").asInt())
                .isZero();
    }

    @Test
    void ohneEmailAdresseGehtDieMahnungPerPost() {
        var kunde = kundeAnlegen(buchhaltung, mandant, ohneEmail("Moser Schreinerei"));
        ueberfaelligeRechnung(kunde, "310.00", 40);
        post(admin, "/api/mahnlaeufe", Map.of());
        var vorschlag = einzigerVorschlag();
        freigeben(buchhaltung, vorschlag);

        var freigegeben =
                get(buchhaltung, "/api/mahnungen/" + vorschlag.get("id").asString());
        assertThat(freigegeben.get("status").asString()).isEqualTo("FREIGEGEBEN");
        assertThat(freigegeben.get("versandweg").asString()).isEqualTo("POST");
        assertThat(pdf(buchhaltung, vorschlag.get("id").asString())).isNotEmpty();

        var versendet = post(
                buchhaltung,
                "/api/mahnungen/" + vorschlag.get("id").asString() + "/postversand",
                Map.of("version", freigegeben.get("version").asLong()));
        assertThat(versendet.get("status").asString()).isEqualTo("VERSENDET");
        assertThat(versendet.get("versendetAm").isNull()).isFalse();
    }

    @Test
    void rollenWerdenImBackendGeprueft() {
        var leser = token("leser", mandant);
        client.post()
                .uri("/api/mahnlaeufe")
                .headers(als(buchhaltung, mandant))
                .exchange()
                .expectStatus()
                .isForbidden();
        client.put()
                .uri("/api/mahnstufen")
                .headers(als(buchhaltung, mandant))
                .body(Map.of("stufen", List.of(stufe("Erinnerung", 10, "0.00"))))
                .exchange()
                .expectStatus()
                .isForbidden();
        var kunde = kundeAnlegen(buchhaltung, mandant, "Rollen Test AG");
        ueberfaelligeRechnung(kunde, "50.00", 30);
        post(admin, "/api/mahnlaeufe", Map.of());
        var vorschlag = einzigerVorschlag();
        client.post()
                .uri("/api/mahnungen/freigabe")
                .headers(als(leser, mandant))
                .body(Map.of(
                        "mahnungen",
                        List.of(Map.of(
                                "id",
                                vorschlag.get("id").asString(),
                                "version",
                                vorschlag.get("version").asLong()))))
                .exchange()
                .expectStatus()
                .isForbidden();
        assertThat(getListe(leser, "/api/mahnstufen").size()).isEqualTo(3);
    }

    @Test
    void mahnstufenFestlegenPrueftReihenfolgeUndVersion() {
        var bisher = getListe(admin, "/api/mahnstufen");
        assertThat(bisher.size()).isEqualTo(3);

        var falscheReihenfolge = List.of(stufe("A", 30, "0.00"), stufe("B", 20, "0.00"));
        client.put()
                .uri("/api/mahnstufen")
                .headers(als(admin, mandant))
                .body(Map.of("stufen", falscheReihenfolge))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("UNGUELTIGER_WERT");

        var unbekannterPlatzhalter = new HashMap<>(stufe("A", 10, "0.00"));
        unbekannterPlatzhalter.put("version", bisher.get(0).get("version").asLong());
        unbekannterPlatzhalter.put("textvorlage", "Rechnung {nummer} ist offen.");
        client.put()
                .uri("/api/mahnstufen")
                .headers(als(admin, mandant))
                .body(Map.of("stufen", List.of(unbekannterPlatzhalter)))
                .exchange()
                .expectStatus()
                .isBadRequest();

        // Ohne Version der bisherigen Stufe gilt die Änderung als veraltet.
        client.put()
                .uri("/api/mahnstufen")
                .headers(als(admin, mandant))
                .body(Map.of("stufen", List.of(stufe("Erinnerung", 10, "0.00"))))
                .exchange()
                .expectStatus()
                .isEqualTo(409);

        var neu = stufenFestlegen(List.of(stufe("Erinnerung", 7, "0.00"), stufe("Mahnung", 21, "15.00")));
        assertThat(neu.size()).isEqualTo(2);
        assertThat(neu.get(1).get("gebuehr").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(neu.get(0).get("mindestabstandTage").asInt()).isZero();
    }

    // --- Hilfen ---

    /** Rechnung ohne MWST, deren Fälligkeit {@code tageUeberfaellig} Tage zurückliegt. */
    private JsonNode ueberfaelligeRechnung(UUID kunde, String betrag, int tageUeberfaellig) {
        var datum = HEUTE.minusDays(tageUeberfaellig + 10L);
        var entwurf = post(
                buchhaltung,
                "/api/rechnungen",
                Map.of(
                        "kundeId",
                        kunde,
                        "rechnungsdatum",
                        datum.toString(),
                        "zahlungsfristTage",
                        10,
                        "waehrung",
                        "CHF",
                        "positionen",
                        List.of(Map.of(
                                "beschreibung",
                                "Wartung",
                                "menge",
                                1,
                                "einzelpreis",
                                new BigDecimal(betrag),
                                "mwstCode",
                                "OHNE"))));
        return post(
                buchhaltung,
                "/api/rechnungen/" + entwurf.get("id").asString() + "/freigabe",
                Map.of("version", entwurf.get("version").asLong()));
    }

    private JsonNode einzigerVorschlag() {
        var vorschlaege = get(buchhaltung, "/api/mahnungen?status=VORSCHLAG").get("inhalt");
        assertThat(vorschlaege.size()).isEqualTo(1);
        return vorschlaege.get(0);
    }

    private JsonNode freigeben(String token, JsonNode vorschlag) {
        return post(
                token,
                "/api/mahnungen/freigabe",
                Map.of(
                        "mahnungen",
                        List.of(Map.of(
                                "id",
                                vorschlag.get("id").asString(),
                                "version",
                                vorschlag.get("version").asLong()))));
    }

    private JsonNode aufStatusWarten(String mahnungId, String status) {
        await().atMost(Duration.ofSeconds(30))
                .until(() -> status.equals(get(buchhaltung, "/api/mahnungen/" + mahnungId)
                        .get("status")
                        .asString()));
        return get(buchhaltung, "/api/mahnungen/" + mahnungId);
    }

    private MimeMessage mailAn(String adresse) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> mailserver.getReceivedMessagesForDomain(adresse).length == 1);
        return mailserver.getReceivedMessagesForDomain(adresse)[0];
    }

    private static Map<String, Object> stufe(String bezeichnung, int tage, String gebuehr) {
        return Map.of(
                "bezeichnung",
                bezeichnung,
                "tageNachFaelligkeit",
                tage,
                "mindestabstandTage",
                7,
                "fristTage",
                10,
                "gebuehr",
                new BigDecimal(gebuehr),
                "textvorlage",
                "Rechnung {rechnungsnummer} ist seit {faelligAm} offen: {offen}.");
    }

    /** Legt die Stufen fest und übernimmt dafür die Versionen der bisherigen Stufen. */
    private JsonNode stufenFestlegen(List<Map<String, Object>> stufen) {
        var bisher = getListe(admin, "/api/mahnstufen");
        var mitVersion = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < stufen.size(); i++) {
            var eingabe = new HashMap<>(stufen.get(i));
            if (i < bisher.size()) {
                eingabe.put("version", bisher.get(i).get("version").asLong());
            }
            mitVersion.add(eingabe);
        }
        return JSON.readTree(client.put()
                .uri("/api/mahnstufen")
                .headers(als(admin, mandant))
                .body(Map.of("stufen", mitVersion))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());
    }

    private static Map<String, Object> mitEmail(String name, String email) {
        var kunde = new HashMap<>(kunde(name));
        kunde.put("email", email);
        return kunde;
    }

    private static Map<String, Object> ohneEmail(String name) {
        var kunde = new HashMap<>(kunde(name));
        kunde.remove("email");
        return kunde;
    }

    private UUID kundeAnlegen(String token, UUID mandant, Map<String, Object> kunde) {
        var antwort = client.post()
                .uri("/api/kunden")
                .headers(als(token, mandant))
                .body(kunde)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return UUID.fromString(JSON.readTree(antwort).get("id").asString());
    }

    private void importieren(byte[] inhalt) {
        var teile = new LinkedMultiValueMap<String, Object>();
        teile.add("datei", new ByteArrayResource(inhalt) {
            @Override
            public String getFilename() {
                return "camt054.xml";
            }
        });
        client.post()
                .uri("/api/zahlungsdateien")
                .headers(als(buchhaltung, mandant))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(teile)
                .exchange()
                .expectStatus()
                .isCreated();
    }

    /** Eine Gutschrift mit QR-Referenz auf die QR-IBAN des Testmandanten; jede Datei und Buchung ist eindeutig. */
    private static byte[] camt(String betrag, String referenz) {
        var kennung = UUID.randomUUID().toString().substring(0, 18);
        var xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.054.001.08">
                  <BkToCstmrDbtCdtNtfctn>
                    <GrpHdr><MsgId>M-%1$s</MsgId><CreDtTm>%2$sT06:30:00+02:00</CreDtTm></GrpHdr>
                    <Ntfctn>
                      <Id>N-%1$s</Id>
                      <CreDtTm>%2$sT06:30:00+02:00</CreDtTm>
                      <Acct><Id><IBAN>%3$s</IBAN></Id><Ccy>CHF</Ccy></Acct>
                      <Ntry>
                        <Amt Ccy="CHF">%4$s</Amt>
                        <CdtDbtInd>CRDT</CdtDbtInd>
                        <Sts><Cd>BOOK</Cd></Sts>
                        <BookgDt><Dt>%2$s</Dt></BookgDt>
                        <ValDt><Dt>%2$s</Dt></ValDt>
                        <AcctSvcrRef>E-%1$s</AcctSvcrRef>
                        <BkTxCd><Domn><Cd>PMNT</Cd><Fmly><Cd>RCDT</Cd><SubFmlyCd>VCOM</SubFmlyCd></Fmly></Domn></BkTxCd>
                        <NtryDtls><TxDtls>
                          <Refs><AcctSvcrRef>T-%1$s</AcctSvcrRef><EndToEndId>NOTPROVIDED</EndToEndId></Refs>
                          <Amt Ccy="CHF">%4$s</Amt>
                          <CdtDbtInd>CRDT</CdtDbtInd>
                          <RltdPties><Dbtr><Pty><Nm>Gasser Treuhand GmbH</Nm></Pty></Dbtr></RltdPties>
                          <RmtInf><Strd><CdtrRefInf>
                            <Tp><CdOrPrtry><Prtry>QRR</Prtry></CdOrPrtry></Tp><Ref>%5$s</Ref>
                          </CdtrRefInf></Strd></RmtInf>
                        </TxDtls></NtryDtls>
                      </Ntry>
                    </Ntfctn>
                  </BkToCstmrDbtCdtNtfctn>
                </Document>
                """.formatted(kennung, HEUTE, QR_IBAN, betrag, referenz);
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(String token, String mahnungId) {
        return client.get()
                .uri("/api/mahnungen/{id}/pdf", mahnungId)
                .headers(als(token, mandant))
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PDF)
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
    }

    private JsonNode get(String token, String pfad) {
        return JSON.readTree(
                antwort(client.get().uri(pfad).headers(als(token, mandant)).exchange()));
    }

    private JsonNode getListe(String token, String pfad) {
        return get(token, pfad);
    }

    private JsonNode post(String token, String pfad, Object body) {
        return JSON.readTree(antwort(
                client.post().uri(pfad).headers(als(token, mandant)).body(body).exchange()));
    }

    private static String antwort(RestTestClient.ResponseSpec antwort) {
        return antwort.expectStatus()
                .is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }
}
