-- Rechnungen mit Positionen, datierte MWST-Sätze und Nummernkreise.
-- Fremdschlüssel laufen über (mandant_id, id): eine Rechnung kann so nie auf Kunde, Satz oder Position
-- eines anderen Mandanten zeigen, auch nicht durch einen Fehler in der Anwendung.

ALTER TABLE kunde ADD CONSTRAINT kunde_mandant_id_eindeutig UNIQUE (mandant_id, id);

-- Fortlaufende Nummern je Mandant, Art und Jahr (Kundennummern: Jahr 0).
CREATE TABLE nummernkreis (
    mandant_id      UUID            NOT NULL REFERENCES mandant (id),
    art             VARCHAR(20)     NOT NULL,
    jahr            INTEGER         NOT NULL,
    letzte_nummer   BIGINT          NOT NULL,
    PRIMARY KEY (mandant_id, art, jahr),
    CONSTRAINT nummernkreis_positiv CHECK (letzte_nummer > 0)
);

CREATE TABLE mwst_satz (
    id              UUID            PRIMARY KEY,
    mandant_id      UUID            NOT NULL REFERENCES mandant (id),
    code            VARCHAR(20)     NOT NULL,
    bezeichnung     VARCHAR(60)     NOT NULL,
    satz            NUMERIC(5,2)    NOT NULL,
    gueltig_ab      DATE            NOT NULL,
    gueltig_bis     DATE,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mwst_satz_code CHECK (code IN ('NORMAL', 'REDUZIERT', 'BEHERBERGUNG', 'OHNE')),
    CONSTRAINT mwst_satz_bereich CHECK (satz >= 0 AND satz < 100),
    CONSTRAINT mwst_satz_zeitraum CHECK (gueltig_bis IS NULL OR gueltig_bis >= gueltig_ab),
    CONSTRAINT mwst_satz_eindeutig UNIQUE (mandant_id, code, gueltig_ab),
    CONSTRAINT mwst_satz_mandant_id_eindeutig UNIQUE (mandant_id, id)
);

CREATE TABLE rechnung (
    id                      UUID            PRIMARY KEY,
    mandant_id              UUID            NOT NULL REFERENCES mandant (id),
    kunde_id                UUID            NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    rechnungsnummer         VARCHAR(20),
    referenz                VARCHAR(27),
    rechnungsdatum          DATE            NOT NULL,
    zahlungsfrist_tage      INTEGER         NOT NULL,
    faellig_am              DATE            NOT NULL,
    waehrung                VARCHAR(3)      NOT NULL,
    bemerkung               VARCHAR(500),
    empfaenger_name         VARCHAR(70)     NOT NULL,
    empfaenger_strasse      VARCHAR(70),
    empfaenger_hausnummer   VARCHAR(16),
    empfaenger_plz          VARCHAR(16)     NOT NULL,
    empfaenger_ort          VARCHAR(35)     NOT NULL,
    empfaenger_land         VARCHAR(2)      NOT NULL,
    netto                   NUMERIC(12,2)   NOT NULL,
    mwst_betrag             NUMERIC(12,2)   NOT NULL,
    rundung                 NUMERIC(12,2)   NOT NULL,
    total                   NUMERIC(12,2)   NOT NULL,
    freigegeben_am          TIMESTAMPTZ,
    storniert_am            TIMESTAMPTZ,
    storno_grund            VARCHAR(200),
    pdf_schluessel          VARCHAR(200),
    erstellt_am             TIMESTAMPTZ     NOT NULL DEFAULT now(),
    geaendert_am            TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT rechnung_kunde FOREIGN KEY (mandant_id, kunde_id) REFERENCES kunde (mandant_id, id),
    CONSTRAINT rechnung_status CHECK (status IN ('ENTWURF', 'OFFEN', 'TEILBEZAHLT', 'BEZAHLT', 'STORNIERT')),
    CONSTRAINT rechnung_waehrung CHECK (waehrung IN ('CHF', 'EUR')),
    CONSTRAINT rechnung_total CHECK (total >= 0),
    CONSTRAINT rechnung_faelligkeit CHECK (faellig_am >= rechnungsdatum),
    CONSTRAINT rechnung_freigabe_vollstaendig CHECK (
        status = 'ENTWURF'
        OR (rechnungsnummer IS NOT NULL AND referenz IS NOT NULL AND freigegeben_am IS NOT NULL)),
    CONSTRAINT rechnung_storno_vollstaendig CHECK (status <> 'STORNIERT' OR storniert_am IS NOT NULL),
    CONSTRAINT rechnung_nummer_eindeutig UNIQUE (mandant_id, rechnungsnummer),
    CONSTRAINT rechnung_referenz_eindeutig UNIQUE (mandant_id, referenz),
    CONSTRAINT rechnung_mandant_id_eindeutig UNIQUE (mandant_id, id)
);

-- rechnung(mandant_id, referenz) deckt der Unique-Constraint ab.
CREATE INDEX rechnung_status_faelligkeit_idx ON rechnung (mandant_id, status, faellig_am);
CREATE INDEX rechnung_kunde_idx ON rechnung (mandant_id, kunde_id);
CREATE INDEX rechnung_datum_idx ON rechnung (mandant_id, rechnungsdatum DESC);

CREATE TABLE rechnungsposition (
    id              UUID            PRIMARY KEY,
    mandant_id      UUID            NOT NULL REFERENCES mandant (id),
    rechnung_id     UUID            NOT NULL,
    position        INTEGER         NOT NULL,
    beschreibung    VARCHAR(200)    NOT NULL,
    menge           NUMERIC(12,3)   NOT NULL,
    einheit         VARCHAR(10),
    einzelpreis     NUMERIC(12,2)   NOT NULL,
    mwst_satz_id    UUID            NOT NULL,
    mwst_code       VARCHAR(20)     NOT NULL,
    mwst_prozent    NUMERIC(5,2)    NOT NULL,
    betrag          NUMERIC(12,2)   NOT NULL,
    CONSTRAINT rechnungsposition_rechnung FOREIGN KEY (mandant_id, rechnung_id)
        REFERENCES rechnung (mandant_id, id) ON DELETE CASCADE,
    CONSTRAINT rechnungsposition_mwst_satz FOREIGN KEY (mandant_id, mwst_satz_id)
        REFERENCES mwst_satz (mandant_id, id),
    CONSTRAINT rechnungsposition_menge CHECK (menge > 0),
    CONSTRAINT rechnungsposition_einzelpreis CHECK (einzelpreis >= 0),
    CONSTRAINT rechnungsposition_betrag CHECK (betrag >= 0),
    -- Aufgeschoben: Hibernate fügt beim Ersetzen der Positionen neue Zeilen vor dem Löschen der alten ein.
    CONSTRAINT rechnungsposition_reihenfolge UNIQUE (rechnung_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX rechnungsposition_rechnung_idx ON rechnungsposition (rechnung_id);
