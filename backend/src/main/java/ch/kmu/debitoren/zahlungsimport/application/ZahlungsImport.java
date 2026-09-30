package ch.kmu.debitoren.zahlungsimport.application;

import ch.kmu.debitoren.shared.dokumente.Dokumentablage;
import ch.kmu.debitoren.shared.domain.Iban;
import ch.kmu.debitoren.shared.domain.Ids;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.mandant.MandantKontext;
import ch.kmu.debitoren.shared.security.AngemeldeterBenutzer;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import ch.kmu.debitoren.zahlungsimport.domain.Gutschrift;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlung;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungRepository;
import ch.kmu.debitoren.zahlungsimport.domain.Zahlungsdatei;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiParser;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiRepository;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsdateiZeile;
import ch.kmu.debitoren.zahlungsimport.domain.ZahlungsimportFehlercode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importiert eine Bankdatei: bekannte Dateien werden abgelehnt, bekannte Buchungen übersprungen, jede neue
 * Gutschrift wird eine Zahlung. Das Original wird unveränderlich abgelegt; nach dem Commit gleicht das Modul
 * Abgleich die Zahlungen über das Ereignis {@link ZahlungenImportiert} ab.
 */
@Service
public class ZahlungsImport {

    private static final DateTimeFormatter ZEITPUNKT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm").withZone(ZoneId.of("Europe/Zurich"));

    private final ZahlungsdateiParser parser;
    private final ZahlungsdateiRepository dateien;
    private final ZahlungRepository zahlungen;
    private final MandantenAbfragen mandanten;
    private final Dokumentablage ablage;
    private final ApplicationEventPublisher ereignisse;
    private final Clock uhr;

    ZahlungsImport(
            ZahlungsdateiParser parser,
            ZahlungsdateiRepository dateien,
            ZahlungRepository zahlungen,
            MandantenAbfragen mandanten,
            Dokumentablage ablage,
            ApplicationEventPublisher ereignisse,
            Clock uhr) {
        this.parser = parser;
        this.dateien = dateien;
        this.zahlungen = zahlungen;
        this.mandanten = mandanten;
        this.ablage = ablage;
        this.ereignisse = ereignisse;
        this.uhr = uhr;
    }

    @PreAuthorize("hasRole('BUCHHALTUNG')")
    @Transactional
    public ZahlungsdateiZeile importieren(String dateiname, byte[] inhalt) {
        var mandant = MandantKontext.erforderlich();
        var hash = sha256(inhalt);
        dateien.findeNachHash(hash).ifPresent(bekannt -> {
            throw bereitsImportiert(bekannt.getDateiname(), bekannt.getImportiertAm());
        });
        var gelesen = parser.lesen(inhalt);
        kontenPruefen(gelesen.konten());

        var bankReferenzen =
                gelesen.gutschriften().stream().map(Gutschrift::bankReferenz).toList();
        var gesehen = new HashSet<>(zahlungen.bekannteBankreferenzen(bankReferenzen));
        var id = Ids.neu();
        var neue = new ArrayList<Zahlung>();
        for (var gutschrift : gelesen.gutschriften()) {
            // Auch innerhalb einer Datei zählt jede Bankreferenz nur einmal.
            if (gesehen.add(gutschrift.bankReferenz())) {
                neue.add(Zahlung.aus(id, gutschrift));
            }
        }
        int uebersprungen = gelesen.gutschriften().size() - neue.size();

        var schluessel = "zahlungsdateien/%s/%s.xml".formatted(mandant, id);
        var datei = new Zahlungsdatei(
                id,
                hash,
                dateiname,
                schluessel,
                gelesen,
                neue.size(),
                uebersprungen,
                Instant.now(uhr),
                AngemeldeterBenutzer.name());
        try {
            dateien.speichern(datei);
            zahlungen.speichernAlle(neue);
        } catch (DataIntegrityViolationException e) {
            // Dieselbe Datei oder dieselben Buchungen wurden gerade gleichzeitig importiert.
            throw bereitsImportiert(datei.getDateiname(), Instant.now(uhr));
        }
        ablage.ablegen(schluessel, inhalt);
        ereignisse.publishEvent(new ZahlungenImportiert(
                mandant, id, neue.stream().map(Zahlung::getId).toList()));
        return new ZahlungsdateiZeile(
                id,
                datei.getDateiname(),
                datei.getNachrichtId(),
                datei.getErstelltVonBankAm(),
                datei.getImportiertAm(),
                datei.getImportiertVon(),
                datei.getAnzahlBuchungen(),
                datei.getAnzahlNeu(),
                datei.getAnzahlUebersprungen(),
                datei.getAnzahlIgnoriert(),
                neue.size(),
                0,
                0);
    }

    /** Eine Datei für ein fremdes Konto ist fast immer ein Versehen beim Hochladen. */
    private void kontenPruefen(Set<String> konten) {
        var eigene = mandanten.aktueller().konten().stream().map(Iban::wert).collect(Collectors.toSet());
        var fremde = konten.stream().filter(konto -> !eigene.contains(konto)).toList();
        if (!fremde.isEmpty()) {
            throw new FachlicherFehler(
                    ZahlungsimportFehlercode.KONTO_UNBEKANNT,
                    "Die Datei betrifft das Konto %s, das nicht zu diesem Mandanten gehört."
                            .formatted(String.join(", ", fremde)));
        }
    }

    private static FachlicherFehler bereitsImportiert(String dateiname, Instant am) {
        return new FachlicherFehler(
                ZahlungsimportFehlercode.DATEI_BEREITS_IMPORTIERT,
                "Diese Datei wurde bereits am %s importiert (%s).".formatted(ZEITPUNKT.format(am), dateiname));
    }

    static String sha256(byte[] inhalt) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(inhalt));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 fehlt in dieser JVM", e);
        }
    }
}
