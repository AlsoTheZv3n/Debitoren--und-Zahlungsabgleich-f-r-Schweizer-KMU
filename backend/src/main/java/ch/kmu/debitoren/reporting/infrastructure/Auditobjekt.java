package ch.kmu.debitoren.reporting.infrastructure;

import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Wie eine {@code _aud}-Tabelle im Audit-Log erscheint: welche Spalten mit welcher Bezeichnung, wie ein Objekt
 * heisst und zu welcher Rechnung es gehört. Technische Spalten bleiben weg.
 *
 * @param mandantSpalte Spalte mit dem Mandanten; beim Mandanten selbst seine ID
 * @param rechnungSpalte Spalte mit der Rechnung, zu der das Objekt gehört; null, wenn es keine gibt
 * @param zusatz zusätzliche Spalten für die Bezeichnung, als SQL mit dem Alias {@code a} für die _aud-Tabelle
 */
record Auditobjekt(
        Objektart art,
        String tabelle,
        String mandantSpalte,
        String rechnungSpalte,
        String zusatz,
        List<Feld> felder,
        Function<Map<String, Object>, String> bezeichnung) {

    /** Ein Feld im Log; {@code beiAnlage}-Felder erscheinen auch beim Anlegen und Löschen. */
    record Feld(String spalte, String name, Wert wert, boolean beiAnlage) {

        String lesbar(Map<String, Object> zeile) {
            return zeile == null ? null : wert.lesbar(zeile.get(spalte), zeile);
        }
    }

    enum Wert {
        TEXT,
        BETRAG,
        DATUM,
        ZEITPUNKT,
        JA_NEIN,
        CODE,
        PROZENT;

        String lesbar(Object wert, Map<String, Object> zeile) {
            if (wert == null || (wert instanceof String text && text.isBlank())) {
                return null;
            }
            return switch (this) {
                case TEXT -> wert.toString();
                case BETRAG -> {
                    var betrag = zahl((BigDecimal) wert, "#,##0.00");
                    yield zeile.get("waehrung") instanceof String waehrung ? waehrung + " " + betrag : betrag;
                }
                case DATUM -> datum(wert);
                case ZEITPUNKT -> ZEITPUNKT_FORMAT.format(((Timestamp) wert).toInstant());
                case JA_NEIN -> Boolean.TRUE.equals(wert) ? "ja" : "nein";
                case CODE -> CODES.getOrDefault(wert.toString(), wert.toString());
                case PROZENT -> zahl((BigDecimal) wert, "0.0#") + " %";
            };
        }
    }

    static final List<Auditobjekt> ALLE = List.of(
            new Auditobjekt(
                    Objektart.RECHNUNG,
                    "rechnung_aud",
                    "mandant_id",
                    "id",
                    "",
                    List.of(
                            new Feld("status", "Status", Wert.CODE, true),
                            new Feld("rechnungsnummer", "Nummer", Wert.TEXT, false),
                            new Feld("empfaenger_name", "Kunde", Wert.TEXT, true),
                            new Feld("rechnungsdatum", "Rechnungsdatum", Wert.DATUM, true),
                            new Feld("faellig_am", "Fällig am", Wert.DATUM, false),
                            new Feld("total", "Total", Wert.BETRAG, true),
                            new Feld("mahngebuehren", "Mahngebühren", Wert.BETRAG, false),
                            new Feld("referenz", "Referenz", Wert.TEXT, false),
                            new Feld("bemerkung", "Bemerkung", Wert.TEXT, false),
                            new Feld("storno_grund", "Stornogrund", Wert.TEXT, false)),
                    zeile -> Objects.requireNonNullElse((String) zeile.get("rechnungsnummer"), "Entwurf") + " · "
                            + zeile.get("empfaenger_name")),
            new Auditobjekt(
                    Objektart.ZUORDNUNG,
                    "zuordnung_aud",
                    "mandant_id",
                    "rechnung_id",
                    ", (select r.rechnungsnummer from rechnung r where r.id = a.rechnung_id) as zusatz_rechnung",
                    List.of(
                            new Feld("betrag", "Betrag", Wert.BETRAG, true),
                            new Feld("art", "Art", Wert.CODE, true),
                            new Feld("regel", "Regel", Wert.CODE, true)),
                    zeile -> "Zahlung zu " + Objects.requireNonNullElse(zeile.get("zusatz_rechnung"), "Rechnung")),
            new Auditobjekt(
                    Objektart.MAHNUNG,
                    "mahnung_aud",
                    "mandant_id",
                    "rechnung_id",
                    "",
                    List.of(
                            new Feld("status", "Status", Wert.CODE, true),
                            new Feld("mahndatum", "Mahndatum", Wert.DATUM, true),
                            new Feld("frist_bis", "Frist", Wert.DATUM, false),
                            new Feld("gebuehr", "Gebühr", Wert.BETRAG, true),
                            new Feld("offener_betrag", "Betrag im Schreiben", Wert.BETRAG, false),
                            new Feld("versandweg", "Versand", Wert.CODE, false),
                            new Feld("empfaenger_email", "E-Mail", Wert.TEXT, false),
                            new Feld("versandfehler", "Versandfehler", Wert.TEXT, false),
                            new Feld("verworfen_grund", "Grund", Wert.TEXT, false),
                            new Feld("gesperrt_bis", "Gesperrt bis", Wert.DATUM, false)),
                    zeile -> zeile.get("bezeichnung") + " zu " + zeile.get("rechnungsnummer")),
            new Auditobjekt(
                    Objektart.MAHNSTUFE,
                    "mahnstufe_aud",
                    "mandant_id",
                    null,
                    "",
                    List.of(
                            new Feld("bezeichnung", "Bezeichnung", Wert.TEXT, true),
                            new Feld("tage_nach_faelligkeit", "Tage nach Fälligkeit", Wert.TEXT, true),
                            new Feld("mindestabstand_tage", "Mindestabstand", Wert.TEXT, false),
                            new Feld("frist_tage", "Zahlungsfrist", Wert.TEXT, false),
                            new Feld("gebuehr", "Gebühr", Wert.BETRAG, true),
                            new Feld("textvorlage", "Text", Wert.TEXT, false)),
                    zeile -> "Stufe " + zeile.get("stufe") + ": " + zeile.get("bezeichnung")),
            new Auditobjekt(
                    Objektart.KUNDE,
                    "kunde_aud",
                    "mandant_id",
                    null,
                    "",
                    List.of(
                            new Feld("name", "Name", Wert.TEXT, true),
                            new Feld("strasse", "Strasse", Wert.TEXT, false),
                            new Feld("hausnummer", "Hausnummer", Wert.TEXT, false),
                            new Feld("plz", "PLZ", Wert.TEXT, false),
                            new Feld("ort", "Ort", Wert.TEXT, true),
                            new Feld("land", "Land", Wert.TEXT, false),
                            new Feld("email", "E-Mail", Wert.TEXT, false),
                            new Feld("zahlungsfrist_tage", "Zahlungsfrist", Wert.TEXT, false)),
                    zeile -> zeile.get("kundennummer") + " · " + zeile.get("name")),
            new Auditobjekt(
                    Objektart.MANDANT,
                    "mandant_aud",
                    "id",
                    null,
                    "",
                    List.of(
                            new Feld("name", "Name", Wert.TEXT, true),
                            new Feld("strasse", "Strasse", Wert.TEXT, false),
                            new Feld("hausnummer", "Hausnummer", Wert.TEXT, false),
                            new Feld("plz", "PLZ", Wert.TEXT, false),
                            new Feld("ort", "Ort", Wert.TEXT, true),
                            new Feld("iban", "IBAN", Wert.TEXT, true),
                            new Feld("qr_iban", "QR-IBAN", Wert.TEXT, true),
                            new Feld("mwst_nummer", "MWST-Nummer", Wert.TEXT, false),
                            new Feld("email", "E-Mail", Wert.TEXT, false),
                            new Feld("rundung_fuenf_rappen", "Rundung auf 5 Rappen", Wert.JA_NEIN, false)),
                    zeile -> String.valueOf(zeile.get("name"))),
            new Auditobjekt(
                    Objektart.MWST_SATZ,
                    "mwst_satz_aud",
                    "mandant_id",
                    null,
                    "",
                    List.of(
                            new Feld("satz", "Satz", Wert.PROZENT, true),
                            new Feld("gueltig_ab", "Gültig ab", Wert.DATUM, true),
                            new Feld("gueltig_bis", "Gültig bis", Wert.DATUM, true)),
                    zeile -> zeile.get("bezeichnung") + " ab " + Wert.DATUM.lesbar(zeile.get("gueltig_ab"), zeile)));

    static Auditobjekt von(Objektart art) {
        return ALLE.stream().filter(objekt -> objekt.art() == art).findFirst().orElseThrow();
    }

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("ENTWURF", "Entwurf"),
            Map.entry("OFFEN", "Offen"),
            Map.entry("TEILBEZAHLT", "Teilbezahlt"),
            Map.entry("BEZAHLT", "Bezahlt"),
            Map.entry("STORNIERT", "Storniert"),
            Map.entry("VORSCHLAG", "Vorschlag"),
            Map.entry("FREIGEGEBEN", "Freigegeben"),
            Map.entry("VERSENDET", "Versendet"),
            Map.entry("VERWORFEN", "Verworfen"),
            Map.entry("AUTO", "automatisch"),
            Map.entry("MANUELL", "von Hand"),
            Map.entry("REFERENZ_EXAKT", "Referenz"),
            Map.entry("BETRAG_UND_KUNDE", "Betrag und Kunde"),
            Map.entry("EMAIL", "E-Mail"),
            Map.entry("POST", "Post"));

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ZEITPUNKT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Zurich"));

    private static String datum(Object wert) {
        var datum = wert instanceof Date sql ? sql.toLocalDate() : (LocalDate) wert;
        return datum.format(DATUM_FORMAT);
    }

    private static String zahl(BigDecimal wert, String muster) {
        var symbole = new DecimalFormatSymbols(Locale.ROOT);
        symbole.setGroupingSeparator('’');
        symbole.setDecimalSeparator('.');
        return new DecimalFormat(muster, symbole).format(wert);
    }
}
