-- Zuordnungen verbinden Zahlungen mit Rechnungen, auch mehrfach: eine Zahlung für mehrere Rechnungen oder
-- mehrere Teilzahlungen für eine Rechnung. Aufheben heisst löschen; die Sichten rechnen dann neu.

CREATE TABLE zuordnung (
    id              UUID            PRIMARY KEY,
    mandant_id      UUID            NOT NULL REFERENCES mandant (id),
    zahlung_id      UUID            NOT NULL,
    rechnung_id     UUID            NOT NULL,
    betrag          NUMERIC(12,2)   NOT NULL,
    waehrung        VARCHAR(3)      NOT NULL,
    art             VARCHAR(10)     NOT NULL,
    regel           VARCHAR(40)     NOT NULL,
    erstellt_von    VARCHAR(100)    NOT NULL,
    erstellt_am     TIMESTAMPTZ     NOT NULL,
    CONSTRAINT zuordnung_zahlung FOREIGN KEY (mandant_id, zahlung_id) REFERENCES zahlung (mandant_id, id),
    CONSTRAINT zuordnung_rechnung FOREIGN KEY (mandant_id, rechnung_id) REFERENCES rechnung (mandant_id, id),
    CONSTRAINT zuordnung_betrag CHECK (betrag > 0),
    CONSTRAINT zuordnung_art CHECK (art IN ('AUTO', 'MANUELL'))
);

CREATE INDEX zuordnung_rechnung_idx ON zuordnung (rechnung_id);
CREATE INDEX zuordnung_zahlung_idx ON zuordnung (zahlung_id);

-- Offene Posten ziehen jetzt die Zuordnungen ab (V004 hatte nur das Total). Spalten und Typen bleiben,
-- damit CREATE OR REPLACE die Sicht ersetzen kann.
CREATE OR REPLACE VIEW v_offene_posten AS
SELECT r.id                                                                     AS rechnung_id,
       r.mandant_id                                                             AS mandant_id,
       r.kunde_id                                                               AS kunde_id,
       r.waehrung                                                               AS waehrung,
       r.faellig_am                                                             AS faellig_am,
       (r.total - COALESCE(z.summe, 0))::NUMERIC(12,2)                          AS offener_betrag,
       GREATEST((now() AT TIME ZONE 'Europe/Zurich')::date - r.faellig_am, 0)   AS tage_ueber_faelligkeit
FROM rechnung r
LEFT JOIN (SELECT rechnung_id, SUM(betrag) AS summe FROM zuordnung GROUP BY rechnung_id) z
       ON z.rechnung_id = r.id
WHERE r.status IN ('OFFEN', 'TEILBEZAHLT');

-- Zahlungen mit berechnetem Restbetrag für Klärungsliste und Importprotokoll.
CREATE VIEW v_zahlungen AS
SELECT zg.id                                                AS zahlung_id,
       zg.mandant_id                                        AS mandant_id,
       zg.zahlungsdatei_id                                  AS zahlungsdatei_id,
       zg.bank_referenz                                     AS bank_referenz,
       zg.buchungsdatum                                     AS buchungsdatum,
       zg.valuta                                            AS valuta,
       zg.betrag                                            AS betrag,
       zg.waehrung                                          AS waehrung,
       (zg.betrag - COALESCE(s.summe, 0))::NUMERIC(12,2)    AS rest,
       zg.referenz                                          AS referenz,
       zg.auftraggeber                                      AS auftraggeber,
       zg.auftraggeber_iban                                 AS auftraggeber_iban,
       zg.mitteilung                                        AS mitteilung,
       zg.status                                            AS status,
       zg.klaerungsgrund                                    AS klaerungsgrund,
       zg.version                                           AS version
FROM zahlung zg
LEFT JOIN (SELECT zahlung_id, SUM(betrag) AS summe FROM zuordnung GROUP BY zahlung_id) s
       ON s.zahlung_id = zg.id;
