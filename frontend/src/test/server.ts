import { setupServer } from 'msw/node'
import { handlers } from '@/mocks/handlers'

/** MSW ersetzt in Tests das Backend; einzelne Tests überschreiben Antworten mit `server.use`. */
export const server = setupServer(...handlers)
