-- Importierte camt.054-Dateien und ihre Gutschriften. Eine Datei (gleicher SHA-256) und eine Buchung
-- (gleiche Bankreferenz) kommen je Mandant nur einmal vor; das Original liegt unveränderlich in der
-- Dokumentablage.

CREATE TABLE zahlungsdatei (
    id                      UUID            PRIMARY KEY,
    mandant_id              UUID            NOT NULL REFERENCES mandant (id),
    sha256                  VARCHAR(64)     NOT NULL,
    dateiname               VARCHAR(255)    NOT NULL,
    speicherort             VARCHAR(200)    NOT NULL,
    nachricht_id            VARCHAR(35)     NOT NULL,
    erstellt_von_bank_am    TIMESTAMPTZ     NOT NULL,
    importiert_am           TIMESTAMPTZ     NOT NULL,
    importiert_von          VARCHAR(100)    NOT NULL,
    anzahl_buchungen        INTEGER         NOT NULL,
    anzahl_neu              INTEGER         NOT NULL,
    anzahl_uebersprungen    INTEGER         NOT NULL,
    anzahl_ignoriert        INTEGER         NOT NULL,
    CONSTRAINT zahlungsdatei_sha256_format CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT zahlungsdatei_anzahlen CHECK (
        anzahl_neu >= 0 AND anzahl_uebersprungen >= 0 AND anzahl_ignoriert >= 0
        AND anzahl_neu + anzahl_uebersprungen = anzahl_buchungen),
    CONSTRAINT zahlungsdatei_eindeutig UNIQUE (mandant_id, sha256),
    CONSTRAINT zahlungsdatei_mandant_id_eindeutig UNIQUE (mandant_id, id)
);

CREATE INDEX zahlungsdatei_importiert_idx ON zahlungsdatei (mandant_id, importiert_am DESC);

-- Eine Gutschrift. Offen heisst: noch nicht abgeglichen; der offene Restbetrag wird nie gespeichert,
-- sondern aus den Zuordnungen berechnet (v_zahlungen).
CREATE TABLE zahlung (
    id                  UUID            PRIMARY KEY,
    mandant_id          UUID            NOT NULL REFERENCES mandant (id),
    zahlungsdatei_id    UUID            NOT NULL,
    bank_referenz       VARCHAR(80)     NOT NULL,
    buchungsdatum       DATE,
    valuta              DATE            NOT NULL,
    betrag              NUMERIC(12,2)   NOT NULL,
    waehrung            VARCHAR(3)      NOT NULL,
    referenz            VARCHAR(35),
    auftraggeber        VARCHAR(140),
    auftraggeber_iban   VARCHAR(34),
    mitteilung          VARCHAR(500),
    status              VARCHAR(20)     NOT NULL,
    klaerungsgrund      VARCHAR(40),
    erstellt_am         TIMESTAMPTZ     NOT NULL DEFAULT now(),
    geaendert_am        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT zahlung_datei FOREIGN KEY (mandant_id, zahlungsdatei_id) REFERENCES zahlungsdatei (mandant_id, id),
    CONSTRAINT zahlung_betrag CHECK (betrag > 0),
    CONSTRAINT zahlung_waehrung CHECK (waehrung ~ '^[A-Z]{3}$'),
    CONSTRAINT zahlung_status CHECK (status IN ('OFFEN', 'ZUGEORDNET', 'KLAERUNG')),
    CONSTRAINT zahlung_klaerung_begruendet CHECK ((status = 'KLAERUNG') = (klaerungsgrund IS NOT NULL)),
    CONSTRAINT zahlung_bank_referenz_eindeutig UNIQUE (mandant_id, bank_referenz),
    CONSTRAINT zahlung_mandant_id_eindeutig UNIQUE (mandant_id, id)
);

CREATE INDEX zahlung_status_idx ON zahlung (mandant_id, status);
CREATE INDEX zahlung_datei_idx ON zahlung (zahlungsdatei_id);
