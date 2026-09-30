/**
 * Farben der Dashboard-Diagramme, geprüft mit dem Palette-Validator gegen die weisse Kartenfläche:
 *
 * - Altersstruktur: geordnete Bänder, also eine Skala in einem Farbton (Akzentblau, OKLCH-Farbton 250), hell
 *   für «nicht fällig» bis dunkel für «über 90 Tage». Monoton in der Helligkeit, Abstand je Stufe >= 0.06, die
 *   hellste Stufe hat 2.1:1 Kontrast zur Fläche (`--ordinal`).
 * - Zahlungseingänge: Hervorhebung statt Kategorien. «Automatisch» trägt den Akzent, alles übrige ein
 *   zurücktretendes Grau; Legende und Tabelle tragen die Werte, nicht die Farbe allein.
 *
 * Status-Farben (rot für überfällig usw.) kommen hier bewusst nicht vor.
 */
export const ALTERSSKALA = ['#7fb6ee', '#579adc', '#2b7ec9', '#0062ac', '#004985'] as const

export const AKZENT = '#0f74c5'
export const ZURUECKTRETEND = '#94a3b8'

/** Diagrammrahmen in den Grautönen der Oberfläche: Linien slate-200, Achsenschrift slate-500. */
export const GITTER = '#e2e8f0'
export const ACHSE = '#cbd5e1'
export const ACHSENSCHRIFT = '#64748b'
export const FLAECHE = '#ffffff'
export const HOVER = '#f1f5f9'
