package ch.kmu.debitoren.reporting.infrastructure;

import ch.kmu.debitoren.reporting.domain.AuditEintrag;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Aenderung;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Aktion;
import ch.kmu.debitoren.reporting.domain.AuditEintrag.Objektart;
import ch.kmu.debitoren.reporting.domain.Auditquelle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Liest das Audit-Log aus den {@code _aud}-Tabellen von Envers. Eine Liste entsteht in zwei Schritten: erst welche
 * Objekte in welcher Revision geändert wurden, dann je Eintrag der Stand davor und danach für die Unterschiede.
 */
@Component
class JdbcAuditLog implements Auditquelle {

    private final JdbcClient jdbc;

    JdbcAuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<AuditEintrag> eintraege(UUID mandant, Objektart objektart, String benutzer, Pageable seite) {
        var auswahl = Auditobjekt.ALLE.stream()
                .map(objekt -> "select '%s' as objektart, id, rev, revtyp from %s where %s = :mandant"
                        .formatted(objekt.art(), objekt.tabelle(), objekt.mandantSpalte()))
                .collect(Collectors.joining(" union all "));
        var filter = """
                where (cast(:objektart as text) is null or e.objektart = cast(:objektart as text))
                  and (cast(:benutzer as text) is null or r.benutzer = cast(:benutzer as text))
                """;
        var art = objektart == null ? null : objektart.name();
        var anzahl = jdbc.sql("select count(*) from (" + auswahl + ") e join revinfo r on r.id = e.rev " + filter)
                .param("mandant", mandant)
                .param("objektart", art, Types.VARCHAR)
                .param("benutzer", benutzer, Types.VARCHAR)
                .query(Long.class)
                .single();
        var kopf = jdbc.sql("select e.objektart, e.id, e.rev, e.revtyp, r.zeitpunkt, r.benutzer from (" + auswahl
                        + ") e join revinfo r on r.id = e.rev " + filter
                        + " order by e.rev desc, e.objektart limit :groesse offset :versatz")
                .param("mandant", mandant)
                .param("objektart", art, Types.VARCHAR)
                .param("benutzer", benutzer, Types.VARCHAR)
                .param("groesse", seite.getPageSize())
                .param("versatz", seite.getOffset())
                .query(JdbcAuditLog::kopf)
                .list();
        return new PageImpl<>(kopf.stream().map(this::eintrag).toList(), seite, anzahl);
    }

    @Override
    public List<AuditEintrag> verlaufDerRechnung(UUID mandant, UUID rechnungId) {
        var auswahl = Auditobjekt.ALLE.stream()
                .filter(objekt -> objekt.rechnungSpalte() != null)
                .map(objekt ->
                        "select '%s' as objektart, id, rev, revtyp from %s where %s = :mandant and %s = :rechnung"
                                .formatted(
                                        objekt.art(),
                                        objekt.tabelle(),
                                        objekt.mandantSpalte(),
                                        objekt.rechnungSpalte()))
                .collect(Collectors.joining(" union all "));
        return jdbc
                .sql("select e.objektart, e.id, e.rev, e.revtyp, r.zeitpunkt, r.benutzer from (" + auswahl
                        + ") e join revinfo r on r.id = e.rev order by e.rev desc, e.objektart")
                .param("mandant", mandant)
                .param("rechnung", rechnungId)
                .query(JdbcAuditLog::kopf)
                .list()
                .stream()
                .map(this::eintrag)
                .toList();
    }

    private AuditEintrag eintrag(Kopf kopf) {
        var objekt = Auditobjekt.von(kopf.objektart());
        var staende = jdbc.sql("select a.*" + objekt.zusatz() + " from " + objekt.tabelle()
                        + " a where a.id = :id and a.rev <= :rev order by a.rev desc limit 2")
                .param("id", kopf.id())
                .param("rev", kopf.revision())
                .query()
                .listOfRows();
        var nachher = staende.getFirst();
        var vorher = staende.size() > 1 ? staende.get(1) : null;
        var aktion = Aktion.ausRevisionstyp(kopf.revtyp());
        return new AuditEintrag(
                kopf.revision(),
                kopf.zeitpunkt(),
                kopf.benutzer(),
                objekt.art(),
                kopf.id(),
                aktion,
                objekt.bezeichnung().apply(nachher),
                objekt.rechnungSpalte() == null ? null : (UUID) nachher.get(objekt.rechnungSpalte()),
                aenderungen(objekt, aktion, vorher, nachher));
    }

    /**
     * Beim Ändern die Felder, die sich unterscheiden. Fehlt der Stand davor, weil das Objekt vor Einführung des
     * Logs entstand, erscheinen wie beim Anlegen die wichtigsten Felder; beim Löschen deren letzter Stand.
     */
    static List<Aenderung> aenderungen(
            Auditobjekt objekt, Aktion aktion, Map<String, Object> vorher, Map<String, Object> nachher) {
        if (aktion == Aktion.GELOESCHT) {
            return objekt.felder().stream()
                    .filter(Auditobjekt.Feld::beiAnlage)
                    .map(feld -> new Aenderung(feld.name(), feld.lesbar(nachher), null))
                    .filter(aenderung -> aenderung.vorher() != null)
                    .toList();
        }
        if (aktion == Aktion.ANGELEGT || vorher == null) {
            return objekt.felder().stream()
                    .filter(Auditobjekt.Feld::beiAnlage)
                    .map(feld -> new Aenderung(feld.name(), null, feld.lesbar(nachher)))
                    .filter(aenderung -> aenderung.nachher() != null)
                    .toList();
        }
        return objekt.felder().stream()
                .map(feld -> new Aenderung(feld.name(), feld.lesbar(vorher), feld.lesbar(nachher)))
                .filter(aenderung -> !Objects.equals(aenderung.vorher(), aenderung.nachher()))
                .toList();
    }

    private static Kopf kopf(ResultSet zeile, int nummer) throws SQLException {
        return new Kopf(
                Objektart.valueOf(zeile.getString("objektart")),
                zeile.getObject("id", UUID.class),
                zeile.getLong("rev"),
                zeile.getInt("revtyp"),
                zeile.getTimestamp("zeitpunkt").toInstant(),
                zeile.getString("benutzer"));
    }

    private record Kopf(Objektart objektart, UUID id, long revision, int revtyp, Instant zeitpunkt, String benutzer) {}
}
