-- Offene Posten werden nie gespeichert, sondern berechnet. Abgleich, Mahnlauf und Altersstruktur lesen
-- aus dieser Sicht. Phase 3 zieht die Zuordnungen ab, Phase 4 rechnet die Mahngebühren hinzu.
CREATE VIEW v_offene_posten AS
SELECT r.id                                                                     AS rechnung_id,
       r.mandant_id                                                             AS mandant_id,
       r.kunde_id                                                               AS kunde_id,
       r.waehrung                                                               AS waehrung,
       r.faellig_am                                                             AS faellig_am,
       r.total                                                                  AS offener_betrag,
       GREATEST((now() AT TIME ZONE 'Europe/Zurich')::date - r.faellig_am, 0)   AS tage_ueber_faelligkeit
FROM rechnung r
WHERE r.status IN ('OFFEN', 'TEILBEZAHLT');
