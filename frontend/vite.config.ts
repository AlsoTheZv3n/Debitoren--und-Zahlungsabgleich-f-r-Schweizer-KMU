/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { msw } from 'msw/vite'
import { defineConfig } from 'vite'

export default defineConfig(({ mode }) => ({
  // Mit `pnpm dev:mock` beantwortet MSW alle API-Aufrufe im Browser, ohne Backend und Keycloak.
  plugins: [react(), tailwindcss(), ...(mode === 'mock' ? [msw({ mode: 'worker-only' })] : [])],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    // 5180 statt 5173: der Port muss zur Redirect-URI im Keycloak-Realm passen.
    port: 5180,
    strictPort: true,
    // Eine Origin wie in Produktion: /api geht ans lokal laufende Backend.
    proxy: { '/api': 'http://localhost:8082' },
  },
  build: {
    // Grundpaket aus React, Router, TanStack Query, OIDC und Radix (gzip rund 175 kB). Werden die
    // Fachseiten gross, laden die Routen sie per lazy nach.
    chunkSizeWarningLimit: 700,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
    // Ganze Abläufe mit Tippen und Netzwerk-Mocks brauchen in jsdom mehr als die üblichen 5 s.
    testTimeout: 20_000,
  },
}))
