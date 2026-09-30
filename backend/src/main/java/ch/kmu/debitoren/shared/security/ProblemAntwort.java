package ch.kmu.debitoren.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Beantwortet 401 und 403 aus der Security-Kette im selben ProblemDetail-Format wie die Controller. Die
 * Bearer-Varianten setzen zusätzlich den Header {@code WWW-Authenticate}.
 */
class ProblemAntwort implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver fehlerbehandlung;
    private final BearerTokenAuthenticationEntryPoint bearerEinstieg = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerVerweigert = new BearerTokenAccessDeniedHandler();

    ProblemAntwort(HandlerExceptionResolver fehlerbehandlung) {
        this.fehlerbehandlung = fehlerbehandlung;
    }

    @Override
    public void commence(HttpServletRequest anfrage, HttpServletResponse antwort, AuthenticationException fehler) {
        bearerEinstieg.commence(anfrage, antwort, fehler);
        fehlerbehandlung.resolveException(anfrage, antwort, null, fehler);
    }

    @Override
    public void handle(HttpServletRequest anfrage, HttpServletResponse antwort, AccessDeniedException fehler) {
        bearerVerweigert.handle(anfrage, antwort, fehler);
        fehlerbehandlung.resolveException(anfrage, antwort, null, fehler);
    }
}
