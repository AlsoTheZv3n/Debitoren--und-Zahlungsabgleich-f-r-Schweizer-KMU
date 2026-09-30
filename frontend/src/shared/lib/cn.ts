import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Verbindet Tailwind-Klassen; spätere Klassen gewinnen bei Konflikten, etwa `px-2` gegen `px-4`. */
export function cn(...klassen: ClassValue[]): string {
  return twMerge(clsx(klassen))
}
