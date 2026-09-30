package ch.kmu.debitoren.shared.mandant;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Bestimmt den Mandanten der Anfrage und bindet ihn für die ganze Verarbeitung an den
 * {@link MandantKontext}.
 *
 * <p>Der Header {@value #HEADER} wählt einen der Mandanten aus dem Token. Ohne Header gilt der einzige
 * Mandant des Tokens; hat der Benutzer mehrere, bleibt der Mandant offen und mandantenbezogene Endpunkte
 * antworten mit {@code MANDANT_FEHLT}.
 *
 * <p>Der Filter läuft als normaler Servlet-Filter nach der Spring-Security-Kette, die Anmeldung steht
 * also schon fest.
 */
@Component
public class MandantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Mandant-Id";

    private final HandlerExceptionResolver fehlerbehandlung;

    MandantFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver fehlerbehandlung) {
        this.fehlerbehandlung = fehlerbehandlung;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest anfrage, HttpServletResponse antwort, FilterChain kette)
            throws ServletException, IOException {
        Optional<UUID> mandant;
        try {
            mandant = waehlen(anfrage.getHeader(HEADER), MandantKontext.erlaubteMandanten());
        } catch (FachlicherFehler fehler) {
            fehlerbehandlung.resolveException(anfrage, antwort, null, fehler);
            return;
        }
        if (mandant.isEmpty()) {
            kette.doFilter(anfrage, antwort);
            return;
        }
        try {
            MandantKontext.berechnenFuer(mandant.get(), () -> {
                kette.doFilter(anfrage, antwort);
                return null;
            });
        } catch (IOException | ServletException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    static Optional<UUID> waehlen(String header, List<UUID> erlaubt) {
        if (header == null || header.isBlank()) {
            return erlaubt.size() == 1 ? Optional.of(erlaubt.getFirst()) : Optional.empty();
        }
        var gewaehlt = MandantClaim.alsUuid(header)
                .orElseThrow(() -> new FachlicherFehler(
                        AllgemeinerFehlercode.MANDANT_UNGUELTIG, HEADER + " ist keine gültige Mandanten-ID."));
        if (!erlaubt.contains(gewaehlt)) {
            throw new FachlicherFehler(
                    AllgemeinerFehlercode.MANDANT_NICHT_ERLAUBT, "Für diesen Mandanten fehlt die Berechtigung.");
        }
        return Optional.of(gewaehlt);
    }
}
