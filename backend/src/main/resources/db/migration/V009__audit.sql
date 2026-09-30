-- Audit-Log mit Hibernate Envers: je Transaktion eine Revision mit Benutzer und Mandant, dazu je auditierter
-- Tabelle eine _aud-Tabelle mit dem Stand nach jeder Änderung. revtyp: 0 angelegt, 1 geändert, 2 gelöscht.
-- Die _aud-Tabellen haben keine Fremdschlüssel auf die Fachtabellen: Gelöschtes bleibt im Log erhalten.
-- Technische Spalten wie Version, Zeitstempel und PDF-Verweise stehen nicht im Log.

CREATE SEQUENCE revinfo_seq;

CREATE TABLE revinfo (
    id              BIGINT          PRIMARY KEY,
    zeitpunkt       TIMESTAMPTZ     NOT NULL,
    benutzer        VARCHAR(100)    NOT NULL,
    mandant_id      UUID
);

CREATE INDEX revinfo_mandant_idx ON revinfo (mandant_id, id DESC);

CREATE TABLE mandant_aud (
    id                      UUID            NOT NULL,
    rev                     BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp                  SMALLINT        NOT NULL,
    name                    VARCHAR(70),
    strasse                 VARCHAR(70),
    hausnummer              VARCHAR(16),
    plz                     VARCHAR(16),
    ort                     VARCHAR(35),
    land                    VARCHAR(2),
    iban                    VARCHAR(21),
    qr_iban                 VARCHAR(21),
    mwst_nummer             VARCHAR(20),
    email                   VARCHAR(254),
    rundung_fuenf_rappen    BOOLEAN,
    PRIMARY KEY (id, rev)
);

CREATE TABLE kunde_aud (
    id                  UUID            NOT NULL,
    rev                 BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp              SMALLINT        NOT NULL,
    mandant_id          UUID,
    kundennummer        VARCHAR(20),
    name                VARCHAR(70),
    strasse             VARCHAR(70),
    hausnummer          VARCHAR(16),
    plz                 VARCHAR(16),
    ort                 VARCHAR(35),
    land                VARCHAR(2),
    email               VARCHAR(254),
    zahlungsfrist_tage  INTEGER,
    PRIMARY KEY (id, rev)
);

CREATE TABLE mwst_satz_aud (
    id              UUID            NOT NULL,
    rev             BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp          SMALLINT        NOT NULL,
    mandant_id      UUID,
    code            VARCHAR(20),
    bezeichnung     VARCHAR(60),
    satz            NUMERIC(5,2),
    gueltig_ab      DATE,
    gueltig_bis     DATE,
    PRIMARY KEY (id, rev)
);

CREATE TABLE rechnung_aud (
    id                      UUID            NOT NULL,
    rev                     BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp                  SMALLINT        NOT NULL,
    mandant_id              UUID,
    kunde_id                UUID,
    status                  VARCHAR(20),
    rechnungsnummer         VARCHAR(20),
    referenz                VARCHAR(27),
    rechnungsdatum          DATE,
    zahlungsfrist_tage      INTEGER,
    faellig_am              DATE,
    waehrung                VARCHAR(3),
    bemerkung               VARCHAR(500),
    empfaenger_name         VARCHAR(70),
    empfaenger_strasse      VARCHAR(70),
    empfaenger_hausnummer   VARCHAR(16),
    empfaenger_plz          VARCHAR(16),
    empfaenger_ort          VARCHAR(35),
    empfaenger_land         VARCHAR(2),
    netto                   NUMERIC(12,2),
    mwst_betrag             NUMERIC(12,2),
    rundung                 NUMERIC(12,2),
    total                   NUMERIC(12,2),
    mahngebuehren           NUMERIC(12,2),
    freigegeben_am          TIMESTAMPTZ,
    storniert_am            TIMESTAMPTZ,
    storno_grund            VARCHAR(200),
    PRIMARY KEY (id, rev)
);

CREATE TABLE zuordnung_aud (
    id              UUID            NOT NULL,
    rev             BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp          SMALLINT        NOT NULL,
    mandant_id      UUID,
    zahlung_id      UUID,
    rechnung_id     UUID,
    betrag          NUMERIC(12,2),
    waehrung        VARCHAR(3),
    art             VARCHAR(10),
    regel           VARCHAR(40),
    erstellt_von    VARCHAR(100),
    erstellt_am     TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

CREATE TABLE mahnstufe_aud (
    id                      UUID            NOT NULL,
    rev                     BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp                  SMALLINT        NOT NULL,
    mandant_id              UUID,
    stufe                   INTEGER,
    bezeichnung             VARCHAR(40),
    tage_nach_faelligkeit   INTEGER,
    mindestabstand_tage     INTEGER,
    frist_tage              INTEGER,
    gebuehr                 NUMERIC(12,2),
    textvorlage             VARCHAR(2000),
    PRIMARY KEY (id, rev)
);

CREATE TABLE mahnung_aud (
    id                  UUID            NOT NULL,
    rev                 BIGINT          NOT NULL REFERENCES revinfo (id),
    revtyp              SMALLINT        NOT NULL,
    mandant_id          UUID,
    rechnung_id         UUID,
    rechnungsnummer     VARCHAR(20),
    kunde_id            UUID,
    kunde_name          VARCHAR(70),
    faellig_am          DATE,
    stufe               INTEGER,
    bezeichnung         VARCHAR(40),
    mahndatum           DATE,
    frist_bis           DATE,
    gebuehr             NUMERIC(12,2),
    waehrung            VARCHAR(3),
    status              VARCHAR(20),
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
    erstellt_am         TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

-- Das Log eines Mandanten, neueste Revision zuerst; der Verlauf einer Rechnung sammelt Rechnung, Zuordnungen und
-- Mahnungen.
CREATE INDEX kunde_aud_mandant_idx ON kunde_aud (mandant_id, rev DESC);
CREATE INDEX mwst_satz_aud_mandant_idx ON mwst_satz_aud (mandant_id, rev DESC);
CREATE INDEX rechnung_aud_mandant_idx ON rechnung_aud (mandant_id, rev DESC);
CREATE INDEX zuordnung_aud_mandant_idx ON zuordnung_aud (mandant_id, rev DESC);
CREATE INDEX mahnstufe_aud_mandant_idx ON mahnstufe_aud (mandant_id, rev DESC);
CREATE INDEX mahnung_aud_mandant_idx ON mahnung_aud (mandant_id, rev DESC);
CREATE INDEX zuordnung_aud_rechnung_idx ON zuordnung_aud (rechnung_id);
CREATE INDEX mahnung_aud_rechnung_idx ON mahnung_aud (rechnung_id);
