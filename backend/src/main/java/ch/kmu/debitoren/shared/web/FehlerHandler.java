package ch.kmu.debitoren.shared.web;

import ch.kmu.debitoren.shared.fehler.AllgemeinerFehlercode;
import ch.kmu.debitoren.shared.fehler.FachlicherFehler;
import ch.kmu.debitoren.shared.fehler.Fehlerart;
import ch.kmu.debitoren.shared.fehler.Fehlercode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Beantwortet jeden Fehler als RFC-9457-ProblemDetail mit fachlichem {@code code}. Clients werten den Code
 * aus, nicht den Text.
 */
@RestControllerAdvice
class FehlerHandler extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    private static final Logger LOG = LoggerFactory.getLogger(FehlerHandler.class);

    @ExceptionHandler(FachlicherFehler.class)
    ResponseEntity<ProblemDetail> fachlich(FachlicherFehler fehler, HttpServletRequest anfrage) {
        return antwort(fehler.code(), fehler.getMessage(), anfrage);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> nichtAngemeldet(AuthenticationException fehler, HttpServletRequest anfrage) {
        return antwort(AllgemeinerFehlercode.NICHT_ANGEMELDET, "Anmeldung fehlt oder das Token ist ungültig.", anfrage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> verweigert(AccessDeniedException fehler, HttpServletRequest anfrage) {
        return antwort(
                AllgemeinerFehlercode.ZUGRIFF_VERWEIGERT, "Die Rolle reicht für diese Aktion nicht aus.", anfrage);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> gleichzeitigGeaendert(
            OptimisticLockingFailureException fehler, HttpServletRequest anfrage) {
        return antwort(
                AllgemeinerFehlercode.GLEICHZEITIG_GEAENDERT,
                "Der Datensatz wurde inzwischen geändert. Bitte neu laden.",
                anfrage);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unerwartet(Exception fehler, HttpServletRequest anfrage) {
        LOG.error("Unerwarteter Fehler bei {} {}", anfrage.getMethod(), anfrage.getRequestURI(), fehler);
        return antwort(AllgemeinerFehlercode.INTERNER_FEHLER, "Ein unerwarteter Fehler ist aufgetreten.", anfrage);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException fehler, HttpHeaders kopf, HttpStatusCode status, WebRequest anfrage) {
        var problem = problem(
                AllgemeinerFehlercode.VALIDIERUNG_FEHLGESCHLAGEN, "Mindestens ein Feld ist ungültig.", pfad(anfrage));
        List<Map<String, String>> felder = fehler.getBindingResult().getFieldErrors().stream()
                .map(feld -> Map.of(
                        "feld", feld.getField(),
                        "meldung", String.valueOf(feld.getDefaultMessage())))
                .toList();
        problem.setProperty("fehler", felder);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    /** Ungültige Parameter, etwa {@code seite=-1}, und ungültige Elemente in Listen. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException fehler, HttpHeaders kopf, HttpStatusCode status, WebRequest anfrage) {
        var problem = problem(
                AllgemeinerFehlercode.VALIDIERUNG_FEHLGESCHLAGEN, "Mindestens ein Wert ist ungültig.", pfad(anfrage));
        List<Map<String, String>> felder = fehler.getParameterValidationResults().stream()
                .flatMap(ergebnis -> ergebnis.getResolvableErrors().stream()
                        .map(meldung -> Map.of(
                                "feld",
                                        String.valueOf(
                                                ergebnis.getMethodParameter().getParameterName()),
                                "meldung", String.valueOf(meldung.getDefaultMessage()))))
                .toList();
        problem.setProperty("fehler", felder);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    /** Ergänzt die Standardfehler von Spring MVC, etwa 404 oder 405, um einen Code. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders kopf, HttpStatusCode status, WebRequest anfrage) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(CODE))) {
            problem.setProperty(
                    CODE,
                    status.value() == HttpStatus.NOT_FOUND.value()
                            ? AllgemeinerFehlercode.NICHT_GEFUNDEN.name()
                            : AllgemeinerFehlercode.ANFRAGE_UNGUELTIG.name());
        }
        return super.createResponseEntity(body, kopf, status, anfrage);
    }

    private static ResponseEntity<ProblemDetail> antwort(Fehlercode code, String detail, HttpServletRequest anfrage) {
        var problem = problem(code, detail, anfrage.getRequestURI());
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    private static ProblemDetail problem(Fehlercode code, String detail, String pfad) {
        var problem = ProblemDetail.forStatusAndDetail(status(code.art()), detail);
        problem.setTitle(code.titel());
        problem.setType(URI.create(
                "urn:debitoren:problem:" + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        if (pfad != null) {
            problem.setInstance(URI.create(pfad));
        }
        problem.setProperty(CODE, code.name());
        return problem;
    }

    private static String pfad(WebRequest anfrage) {
        return anfrage instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : null;
    }

    static HttpStatus status(Fehlerart art) {
        return switch (art) {
            case UNGUELTIG -> HttpStatus.BAD_REQUEST;
            case NICHT_ANGEMELDET -> HttpStatus.UNAUTHORIZED;
            case VERBOTEN -> HttpStatus.FORBIDDEN;
            case NICHT_GEFUNDEN -> HttpStatus.NOT_FOUND;
            case KONFLIKT -> HttpStatus.CONFLICT;
            case INTERN -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
