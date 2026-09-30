import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { RouterProvider, type createBrowserRouter } from 'react-router'

/** Gemeinsamer Kern für Betrieb und Mock-Modus; die Anmeldung legt der Aufrufer darum. */
export function Anwendung({
  router,
  queryClient,
}: {
  router: ReturnType<typeof createBrowserRouter>
  queryClient: QueryClient
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
