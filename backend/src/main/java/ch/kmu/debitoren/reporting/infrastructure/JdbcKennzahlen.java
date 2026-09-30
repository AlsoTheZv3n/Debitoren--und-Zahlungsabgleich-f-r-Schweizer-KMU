package ch.kmu.debitoren.reporting.infrastructure;

import ch.kmu.debitoren.reporting.domain.Altersstruktur;
import ch.kmu.debitoren.reporting.domain.Altersstruktur.Altersband;
import ch.kmu.debitoren.reporting.domain.Altersstruktur.Band;
import ch.kmu.debitoren.reporting.domain.Kennzahlenquelle;
import ch.kmu.debitoren.reporting.domain.Monatseingang;
import ch.kmu.debitoren.reporting.domain.Uebersicht;
import ch.kmu.debitoren.reporting.domain.Uebersicht.Automatisierung;
import ch.kmu.debitoren.reporting.domain.Uebersicht.Import;
import ch.kmu.debitoren.reporting.domain.Uebersicht.Mahnlaufinfo;
import ch.kmu.debitoren.reporting.domain.Uebersicht.Summe;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Liest die Kennzahlen mit SQL aus den Sichten und Tabellen der Fachmodule. Hibernate filtert hier nicht nach
 * Mandant; jede Abfrage schränkt deshalb selbst auf {@code mandant_id} ein.
 */
@Component
class JdbcKennzahlen implements Kennzahlenquelle {

    /** Zeitraum, für den der Anteil automatisch zugeordneter Zahlungen gilt. */
    static final int TAGE_AUTOMATISIERUNG = 30;

    private static final BigDecimal NULLBETRAG = BigDecimal.ZERO.setScale(2);

    private final JdbcClient jdbc;

    JdbcKennzahlen(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Uebersicht uebersicht(UUID mandant, LocalDate heute) {
        var jeWaehrung = jdbc.sql("""
                        select waehrung,
                               count(*) as anzahl,
                               sum(offener_betrag) as betrag,
                               count(*) filter (where faellig_am < :heute) as anzahl_ueberfaellig,
                               coalesce(sum(offener_betrag) filter (where faellig_am < :heute), 0) as betrag_ueberfaellig
                        from v_offene_posten
                        where mandant_id = :mandant and offener_betrag > 0
                        group by waehrung
                        order by waehrung
                        """)
                .param("mandant", mandant)
                .param("heute", heute)
                .query((zeile, nummer) -> {
                    var waehrung = Currency.getInstance(zeile.getString("waehrung"));
                    return List.of(
                            new Summe(waehrung, zeile.getBigDecimal("betrag"), zeile.getLong("anzahl")),
                            new Summe(
                                    waehrung,
                                    zeile.getBigDecimal("betrag_ueberfaellig"),
                                    zeile.getLong("anzahl_ueberfaellig")));
                })
                .list();
        var offen = jeWaehrung.stream().map(List::getFirst).toList();
        var ueberfaellig = jeWaehrung.stream()
                .map(List::getLast)
                .filter(summe -> summe.anzahl() > 0)
                .toList();

        var inKlaerung = jdbc.sql("""
                        select waehrung, count(*) as anzahl, sum(rest) as betrag
                        from v_zahlungen
                        where mandant_id = :mandant and status = 'KLAERUNG'
                        group by waehrung
                        order by waehrung
                        """)
                .param("mandant", mandant)
                .query(JdbcKennzahlen::summe)
                .list();

        var mahnvorschlaege =
                anzahl("select count(*) from mahnung where mandant_id = :mandant and status = 'VORSCHLAG'", mandant);
        var entwuerfe =
                anzahl("select count(*) from rechnung where mandant_id = :mandant and status = 'ENTWURF'", mandant);

        var automatisierung = jdbc.sql("""
                        select count(*) as zahlungen,
                               count(*) filter (where exists (
                                   select 1 from zuordnung zu where zu.zahlung_id = z.id and zu.art = 'AUTO'))
                                   as automatisch
                        from zahlung z
                        where z.mandant_id = :mandant and z.valuta > :seit
                        """)
                .param("mandant", mandant)
                .param("seit", heute.minusDays(TAGE_AUTOMATISIERUNG))
                .query((zeile, nummer) -> new Automatisierung(zeile.getLong("zahlungen"), zeile.getLong("automatisch")))
                .single();

        var importe = jdbc.sql("""
                        select d.id, d.dateiname, d.importiert_am, d.importiert_von, d.anzahl_neu,
                               (select count(*) from zahlung z
                                where z.zahlungsdatei_id = d.id and z.status = 'KLAERUNG') as in_klaerung
                        from zahlungsdatei d
                        where d.mandant_id = :mandant
                        order by d.importiert_am desc
                        limit 5
                        """)
                .param("mandant", mandant)
                .query((zeile, nummer) -> new Import(
                        zeile.getObject("id", UUID.class),
                        zeile.getString("dateiname"),
                        zeile.getTimestamp("importiert_am").toInstant(),
                        zeile.getString("importiert_von"),
                        zeile.getInt("anzahl_neu"),
                        zeile.getLong("in_klaerung")))
                .list();

        var mahnlauf = jdbc.sql("""
                        select ausgefuehrt_am, ausgeloest_von, vorschlaege
                        from mahnlauf
                        where mandant_id = :mandant
                        order by ausgefuehrt_am desc
                        limit 1
                        """)
                .param("mandant", mandant)
                .query((zeile, nummer) -> new Mahnlaufinfo(
                        zeile.getTimestamp("ausgefuehrt_am").toInstant(),
                        zeile.getString("ausgeloest_von"),
                        zeile.getInt("vorschlaege")))
                .optional()
                .orElse(null);

        return new Uebersicht(
                heute, offen, ueberfaellig, inKlaerung, mahnvorschlaege, entwuerfe, automatisierung, importe, mahnlauf);
    }

    @Override
    public Altersstruktur altersstruktur(UUID mandant, LocalDate heute) {
        var posten = jdbc.sql("""
                        select waehrung, (:heute - faellig_am) as tage, offener_betrag
                        from v_offene_posten
                        where mandant_id = :mandant and offener_betrag > 0
                        """)
                .param("mandant", mandant)
                .param("heute", heute)
                .query((zeile, nummer) -> new Posten(
                        zeile.getString("waehrung"), zeile.getLong("tage"), zeile.getBigDecimal("offener_betrag")))
                .list();

        var jeWaehrung = new TreeMap<String, EnumMap<Band, Altersband>>();
        jeWaehrung.put("CHF", leereBaender());
        for (var einzeln : posten) {
            var baender = jeWaehrung.computeIfAbsent(einzeln.waehrung(), w -> leereBaender());
            var band = Band.fuer(einzeln.tage());
            var bisher = baender.get(band);
            baender.put(band, new Altersband(band, bisher.betrag().add(einzeln.offen()), bisher.anzahl() + 1));
        }

        var waehrungen = jeWaehrung.entrySet().stream()
                .map(eintrag -> new Altersstruktur.Waehrung(
                        Currency.getInstance(eintrag.getKey()),
                        List.copyOf(eintrag.getValue().values()),
                        eintrag.getValue().values().stream()
                                .map(Altersband::betrag)
                                .reduce(NULLBETRAG, BigDecimal::add)))
                .toList();
        return new Altersstruktur(heute, waehrungen);
    }

    @Override
    public List<Monatseingang> zahlungseingaenge(UUID mandant, LocalDate von) {
        return jdbc.sql("""
                        select to_char(z.valuta, 'YYYY-MM') as monat,
                               z.waehrung,
                               count(*) as anzahl,
                               sum(z.betrag) as betrag,
                               coalesce(sum(a.automatisch), 0) as automatisch
                        from zahlung z
                        left join (select zahlung_id, sum(betrag) as automatisch
                                   from zuordnung
                                   where art = 'AUTO' and mandant_id = :mandant
                                   group by zahlung_id) a on a.zahlung_id = z.id
                        where z.mandant_id = :mandant and z.valuta >= :von
                        group by 1, 2
                        order by 1, 2
                        """)
                .param("mandant", mandant)
                .param("von", von)
                .query((zeile, nummer) -> new Monatseingang(
                        YearMonth.parse(zeile.getString("monat")),
                        Currency.getInstance(zeile.getString("waehrung")),
                        zeile.getBigDecimal("betrag").setScale(2),
                        zeile.getBigDecimal("automatisch").setScale(2),
                        zeile.getLong("anzahl")))
                .list();
    }

    private long anzahl(String sql, UUID mandant) {
        return jdbc.sql(sql).param("mandant", mandant).query(Long.class).single();
    }

    private static Summe summe(ResultSet zeile, int nummer) throws SQLException {
        return new Summe(
                Currency.getInstance(zeile.getString("waehrung")),
                zeile.getBigDecimal("betrag"),
                zeile.getLong("anzahl"));
    }

    private record Posten(String waehrung, long tage, BigDecimal offen) {}

    private static EnumMap<Band, Altersband> leereBaender() {
        var baender = new EnumMap<Band, Altersband>(Band.class);
        for (var band : Band.values()) {
            baender.put(band, new Altersband(band, NULLBETRAG, 0));
        }
        return baender;
    }
}
