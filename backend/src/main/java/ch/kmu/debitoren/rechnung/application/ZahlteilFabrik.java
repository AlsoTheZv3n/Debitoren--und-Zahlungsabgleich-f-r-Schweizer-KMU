package ch.kmu.debitoren.rechnung.application;

import ch.kmu.debitoren.rechnung.domain.Empfaenger;
import ch.kmu.debitoren.rechnung.domain.Mahnangaben;
import ch.kmu.debitoren.rechnung.domain.QrReferenz;
import ch.kmu.debitoren.rechnung.domain.Rechnung;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungsangaben;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Rechnungssteller;
import ch.kmu.debitoren.rechnung.domain.Rechnungsdarstellung.Zahlteil;
import ch.kmu.debitoren.rechnung.domain.Zahlungsreferenz;
import ch.kmu.debitoren.shared.domain.Geld;
import ch.kmu.debitoren.stammdaten.application.KundeInfo;
import ch.kmu.debitoren.stammdaten.application.MandantenAbfragen;
import java.util.Currency;
import org.springframework.stereotype.Component;

/** Setzt den QR-Zahlteil aus Mandant, Kunde und Rechnung zusammen. */
@Component
class ZahlteilFabrik {

    /**
     * Platzhalter für Vorschauen, solange die Rechnung noch keine Referenz hat. Die Oberfläche kennzeichnet
     * solche Zahlteile deutlich als Vorschau.
     */
    static final QrReferenz VORSCHAU_REFERENZ = QrReferenz.aus("1");

    private final MandantenAbfragen mandanten;

    ZahlteilFabrik(MandantenAbfragen mandanten) {
        this.mandanten = mandanten;
    }

    Rechnungssteller steller() {
        var mandant = mandanten.aktueller();
        return new Rechnungssteller(
                mandant.name(), mandant.adresse(), mandant.zahlungskonto(), mandant.mwstNummer(), mandant.email());
    }

    Zahlteil fuer(Rechnung rechnung, Rechnungssteller steller, KundeInfo kunde) {
        var freigegeben = rechnung.getRechnungsnummer() != null;
        var angaben = freigegeben
                ? new Rechnungsangaben(
                        rechnung.getRechnungsnummer(),
                        rechnung.getRechnungsdatum(),
                        kunde.kundennummer(),
                        steller.mwstNummer(),
                        rechnung.summen().mwst(),
                        rechnung.getZahlungsfristTage())
                : null;
        return new Zahlteil(
                steller,
                rechnung.getWaehrung(),
                rechnung.getTotal().istPositiv() ? rechnung.getTotal() : null,
                rechnung.getEmpfaenger(),
                freigegeben ? rechnung.getReferenz() : vorschauReferenz(steller),
                freigegeben ? "Rechnung " + rechnung.getRechnungsnummer() : "Vorschau",
                angaben);
    }

    /** Zahlteil eines Mahnschreibens: der offene Betrag samt Gebühren unter der Referenz der Rechnung. */
    Zahlteil fuerMahnung(Rechnung rechnung, Rechnungssteller steller, Empfaenger zahler, Mahnangaben angaben) {
        return new Zahlteil(
                steller,
                rechnung.getWaehrung(),
                angaben.offen().istPositiv() ? angaben.offen() : null,
                zahler,
                rechnung.getReferenz(),
                "%s zu Rechnung %s".formatted(angaben.titel(), rechnung.getRechnungsnummer()),
                null);
    }

    Zahlteil vorschau(Rechnungssteller steller, Currency waehrung, Geld total, Empfaenger zahler) {
        return new Zahlteil(
                steller,
                waehrung,
                total.istPositiv() ? total : null,
                zahler,
                vorschauReferenz(steller),
                "Vorschau",
                null);
    }

    /** Mit QR-IBAN ist eine Referenz Pflicht, mit normaler IBAN darf sie fehlen. */
    private static Zahlungsreferenz vorschauReferenz(Rechnungssteller steller) {
        return steller.konto().istQrIban() ? VORSCHAU_REFERENZ : null;
    }
}
