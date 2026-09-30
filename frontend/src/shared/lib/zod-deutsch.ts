import { z } from 'zod'

/**
 * Meldungen der Formularvalidierung auf Deutsch; einmal beim Start und in den Tests laden. Fehlende
 * Pflichtangaben heissen schlicht «Pflichtfeld» statt eines technischen Typfehlers.
 */
z.config(z.locales.de())
z.config({
  customError: (problem) =>
    problem.code === 'invalid_type' && (problem.input === undefined || problem.input === '')
      ? 'Pflichtfeld'
      : undefined,
})
