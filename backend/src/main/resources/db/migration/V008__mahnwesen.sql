-- Mahnwesen: Stufen je Mandant, Mahnungen vom Vorschlag bis zum Versand, das Protokoll der Mahnläufe und die
-- Sperre für geplante Jobs. Die Gebühr einer freigegebenen Mahnung belastet die Rechnung; der offene Posten
-- rechnet sie ab jetzt hinzu.

ALTER TABLE rechnung ADD COLUMN mahngebuehren NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE rechnung ADD CONSTRAINT rechnung_mahngebuehren CHECK (mahngebuehren >= 0);

-- Total plus Mahngebühren minus Zuordnungen (V007 zog nur die Zuordnungen ab). Spalten und Typen bleiben.
CREATE OR REPLACE VIEW v_offene_posten AS
SELECT r.id                                                                     AS rechnung_id,
       r.mandant_id                                                             AS mandant_id,
       r.kunde_id                                                               AS kunde_id,
       r.waehrung                                                               AS waehrung,
       r.faellig_am                                                             AS faellig_am,
       (r.total + r.mahngebuehren - COALESCE(z.summe, 0))::NUMERIC(12,2)        AS offener_betrag,
       GREATEST((now() AT TIME ZONE 'Europe/Zurich')::date - r.faellig_am, 0)   AS tage_ueber_faelligkeit
FROM rechnung r
LEFT JOIN (SELECT rechnung_id, SUM(betrag) AS summe FROM zuordnung GROUP BY rechnung_id) z
       ON z.rechnung_id = r.id
WHERE r.status IN ('OFFEN', 'TEILBEZAHLT');

-- Stufen in fester Reihenfolge; die Stufe n+1 folgt frühestens «mindestabstand_tage» nach Stufe n.
CREATE TABLE mahnstufe (
    id                      UUID            PRIMARY KEY,
    mandant_id              UUID            NOT NULL REFERENCES mandant (id),
    stufe                   INTEGER         NOT NULL,
    bezeichnung             VARCHAR(40)     NOT NULL,
    tage_nach_faelligkeit   INTEGER         NOT NULL,
    mindestabstand_tage     INTEGER         NOT NULL,
    frist_tage              INTEGER         NOT NULL,
    gebuehr                 NUMERIC(12,2)   NOT NULL,
    textvorlage             VARCHAR(2000)   NOT NULL,
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mahnstufe_stufe CHECK (stufe BETWEEN 1 AND 5),
    CONSTRAINT mahnstufe_tage CHECK (tage_nach_faelligkeit BETWEEN 1 AND 365),
    CONSTRAINT mahnstufe_abstand CHECK (mindestabstand_tage BETWEEN 0 AND 365),
    CONSTRAINT mahnstufe_frist CHECK (frist_tage BETWEEN 1 AND 90),
    CONSTRAINT mahnstufe_gebuehr CHECK (gebuehr BETWEEN 0 AND 1000),
    CONSTRAINT mahnstufe_eindeutig UNIQUE (mandant_id, stufe)
);

-- Rechnungsnummer, Kunde und Fälligkeit stehen als Stand der Mahnung hier; so bleibt die Liste auch ohne
-- Rechnungsmodul lesbar. Die Beträge halten fest, was im Schreiben steht, und entstehen bei der Freigabe.
CREATE TABLE mahnung (
    id                  UUID            PRIMARY KEY,
    mandant_id          UUID            NOT NULL REFERENCES mandant (id),
    rechnung_id         UUID            NOT NULL,
    rechnungsnummer     VARCHAR(20)     NOT NULL,
    kunde_id            UUID            NOT NULL,
    kunde_name          VARCHAR(70)     NOT NULL,
    faellig_am          DATE            NOT NULL,
    stufe               INTEGER         NOT NULL,
    bezeichnung         VARCHAR(40)     NOT NULL,
    mahndatum           DATE            NOT NULL,
    frist_bis           DATE            NOT NULL,
    gebuehr             NUMERIC(12,2)   NOT NULL,
    waehrung            VARCHAR(3)      NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    rechnungsbetrag     NUMERIC(12,2),
    bezahlt             NUMERIC(12,2),
    mahngebuehren       NUMERIC(12,2),
    offener_betrag      NUMERIC(12,2),
    brieftext           VARCHAR(2000),
    versandweg          VARCHAR(10),
    empfaenger_email    VARCHAR(254),
    freigegeben_am      TIMESTAMPTZ,
    freigegeben_von     VARCHAR(100),
    versendet_am        TIMESTAMPTZ,
    versandfehler       VARCHAR(500),
    verworfen_am        TIMESTAMPTZ,
    verworfen_von       VARCHAR(100),
    verworfen_grund     VARCHAR(200),
    gesperrt_bis        DATE,
    pdf_schluessel      VARCHAR(200),
    erstellt_am         TIMESTAMPTZ     NOT NULL,
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mahnung_rechnung FOREIGN KEY (mandant_id, rechnung_id) REFERENCES rechnung (mandant_id, id),
    CONSTRAINT mahnung_status CHECK (status IN ('VORSCHLAG', 'FREIGEGEBEN', 'VERSENDET', 'VERWORFEN')),
    CONSTRAINT mahnung_versandweg CHECK (versandweg IS NULL OR versandweg IN ('EMAIL', 'POST')),
    CONSTRAINT mahnung_stufe CHECK (stufe BETWEEN 1 AND 5),
    CONSTRAINT mahnung_gebuehr CHECK (gebuehr >= 0),
    CONSTRAINT mahnung_frist CHECK (frist_bis >= mahndatum),
    CONSTRAINT mahnung_waehrung CHECK (waehrung IN ('CHF', 'EUR')),
    CONSTRAINT mahnung_freigabe_vollstaendig CHECK (
        status IN ('VORSCHLAG', 'VERWORFEN')
        OR (freigegeben_am IS NOT NULL AND freigegeben_von IS NOT NULL AND offener_betrag IS NOT NULL
            AND brieftext IS NOT NULL AND versandweg IS NOT NULL)),
    CONSTRAINT mahnung_versand_vollstaendig CHECK (status <> 'VERSENDET' OR versendet_am IS NOT NULL),
    CONSTRAINT mahnung_verworfen_vollstaendig CHECK (status <> 'VERWORFEN' OR verworfen_am IS NOT NULL)
);

-- Je Rechnung höchstens ein offener Vorschlag, und jede Stufe gibt es nur einmal; Verworfenes zählt nicht.
CREATE UNIQUE INDEX mahnung_ein_vorschlag ON mahnung (rechnung_id) WHERE status = 'VORSCHLAG';
CREATE UNIQUE INDEX mahnung_stufe_einmal ON mahnung (rechnung_id, stufe) WHERE status <> 'VERWORFEN';
CREATE INDEX mahnung_status_idx ON mahnung (mandant_id, status, mahndatum DESC);
CREATE INDEX mahnung_rechnung_idx ON mahnung (rechnung_id);

-- Mahnungen mit dem heute offenen Betrag der Rechnung, für die Liste der Vorschläge.
CREATE VIEW v_mahnungen AS
SELECT m.id                         AS mahnung_id,
       m.mandant_id                 AS mandant_id,
       m.version                    AS version,
       m.rechnung_id                AS rechnung_id,
       m.rechnungsnummer            AS rechnungsnummer,
       m.kunde_id                   AS kunde_id,
       m.kunde_name                 AS kunde_name,
       m.faellig_am                 AS faellig_am,
       m.stufe                      AS stufe,
       m.bezeichnung                AS bezeichnung,
       m.mahndatum                  AS mahndatum,
       m.frist_bis                  AS frist_bis,
       m.gebuehr                    AS gebuehr,
       m.waehrung                   AS waehrung,
       m.status                     AS status,
       m.offener_betrag             AS offener_betrag,
       COALESCE(o.offener_betrag, 0)::NUMERIC(12,2) AS aktuell_offen,
       m.versandweg                 AS versandweg,
       m.empfaenger_email           AS empfaenger_email,
       m.freigegeben_am             AS freigegeben_am,
       m.freigegeben_von            AS freigegeben_von,
       m.versendet_am               AS versendet_am,
       m.versandfehler              AS versandfehler,
       m.verworfen_am               AS verworfen_am,
       m.verworfen_von              AS verworfen_von,
       m.verworfen_grund            AS verworfen_grund,
       m.gesperrt_bis               AS gesperrt_bis,
       m.erstellt_am                AS erstellt_am
FROM mahnung m
LEFT JOIN v_offene_posten o ON o.rechnung_id = m.rechnung_id;

-- Jeder Lauf, automatisch oder von Hand; zeigt, wann zuletzt gemahnt wurde.
CREATE TABLE mahnlauf (
    id                  UUID            PRIMARY KEY,
    mandant_id          UUID            NOT NULL REFERENCES mandant (id),
    stichtag            DATE            NOT NULL,
    ausgefuehrt_am      TIMESTAMPTZ     NOT NULL,
    ausgeloest_von      VARCHAR(100)    NOT NULL,
    geprueft            INTEGER         NOT NULL,
    vorschlaege         INTEGER         NOT NULL,
    verworfen           INTEGER         NOT NULL,
    CONSTRAINT mahnlauf_anzahlen CHECK (geprueft >= 0 AND vorschlaege >= 0 AND verworfen >= 0)
);

CREATE INDEX mahnlauf_ausgefuehrt_idx ON mahnlauf (mandant_id, ausgefuehrt_am DESC);

-- ShedLock: Bei mehreren Instanzen läuft ein geplanter Job, etwa der tägliche Mahnlauf, nur einmal.
-- Zeitpunkte ohne Zeitzone, wie ShedLock sie mit usingDbTime() in UTC schreibt.
CREATE TABLE shedlock (
    name        VARCHAR(64)     PRIMARY KEY,
    lock_until  TIMESTAMP       NOT NULL,
    locked_at   TIMESTAMP       NOT NULL,
    locked_by   VARCHAR(255)    NOT NULL
);
