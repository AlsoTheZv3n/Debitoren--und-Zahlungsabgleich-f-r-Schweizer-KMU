package ch.kmu.debitoren.shared.fehler;

import java.util.Objects;

/** Erwarteter Fehler mit fachlichem Code; wird als RFC-9457-ProblemDetail beantwortet. */
public class FachlicherFehler extends RuntimeException {

    private final transient Fehlercode code;

    public FachlicherFehler(Fehlercode code, String meldung) {
        super(meldung);
        this.code = Objects.requireNonNull(code, "code");
    }

    public Fehlercode code() {
        return code;
    }
}
