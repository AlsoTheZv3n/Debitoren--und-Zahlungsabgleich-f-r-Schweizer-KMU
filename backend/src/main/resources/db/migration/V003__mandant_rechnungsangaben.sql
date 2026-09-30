-- Angaben des Mandanten, die auf jeder Rechnung stehen, und die abschaltbare Rundung auf 5 Rappen.
ALTER TABLE mandant ADD COLUMN mwst_nummer VARCHAR(20);
ALTER TABLE mandant ADD COLUMN email VARCHAR(254);
ALTER TABLE mandant ADD COLUMN rundung_fuenf_rappen BOOLEAN NOT NULL DEFAULT TRUE;
