-- Stammdaten: Mandanten und Kunden.
-- Feldlängen folgen dem QR-Zahlteil (Name 70, Strasse 70, Hausnummer 16, PLZ 16, Ort 35, Land 2).
-- IDs sind UUIDs der Version 7, erzeugt von der Anwendung.

CREATE TABLE mandant (
    id              UUID            PRIMARY KEY,
    name            VARCHAR(70)     NOT NULL,
    strasse         VARCHAR(70),
    hausnummer      VARCHAR(16),
    plz             VARCHAR(16)     NOT NULL,
    ort             VARCHAR(35)     NOT NULL,
    land            VARCHAR(2)      NOT NULL DEFAULT 'CH',
    iban            VARCHAR(21)     NOT NULL,
    qr_iban         VARCHAR(21),
    erstellt_am     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    geaendert_am    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT mandant_land_iso CHECK (land ~ '^[A-Z]{2}$'),
    CONSTRAINT mandant_iban_format CHECK (iban ~ '^(CH|LI)[0-9]{7}[0-9A-Z]{12}$'),
    -- QR-IBAN: Institutsidentifikation (Stellen 5 bis 9) im Bereich 30000 bis 31999
    CONSTRAINT mandant_qr_iban_format CHECK (qr_iban IS NULL OR qr_iban ~ '^(CH|LI)[0-9]{2}3[01][0-9]{3}[0-9A-Z]{12}$'),
    CONSTRAINT mandant_iban_keine_qr_iban CHECK (iban !~ '^(CH|LI)[0-9]{2}3[01]')
);

CREATE TABLE kunde (
    id                  UUID            PRIMARY KEY,
    mandant_id          UUID            NOT NULL REFERENCES mandant (id),
    kundennummer        VARCHAR(20)     NOT NULL,
    name                VARCHAR(70)     NOT NULL,
    strasse             VARCHAR(70),
    hausnummer          VARCHAR(16),
    plz                 VARCHAR(16)     NOT NULL,
    ort                 VARCHAR(35)     NOT NULL,
    land                VARCHAR(2)      NOT NULL DEFAULT 'CH',
    email               VARCHAR(254),
    zahlungsfrist_tage  INTEGER         NOT NULL DEFAULT 30,
    erstellt_am         TIMESTAMPTZ     NOT NULL DEFAULT now(),
    geaendert_am        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT kunde_kundennummer_eindeutig UNIQUE (mandant_id, kundennummer),
    CONSTRAINT kunde_land_iso CHECK (land ~ '^[A-Z]{2}$'),
    CONSTRAINT kunde_zahlungsfrist CHECK (zahlungsfrist_tage BETWEEN 0 AND 365)
);

CREATE INDEX kunde_mandant_name_idx ON kunde (mandant_id, name);
