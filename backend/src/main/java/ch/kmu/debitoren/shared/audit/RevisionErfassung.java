package ch.kmu.debitoren.shared.audit;

import ch.kmu.debitoren.shared.mandant.MandantKontext;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Füllt jede neue Revision mit Benutzer und Mandant. Envers erzeugt den Listener selbst, er ist also keine
 * Spring-Bean; Benutzer und Mandant stammen aus dem Kontext des laufenden Threads.
 */
public class RevisionErfassung implements RevisionListener {

    /** Steht im Log, wenn eine Änderung ohne Anmeldung entsteht, etwa bei einer Migration von Hand. */
    static final String UNBEKANNT = "unbekannt";

    @Override
    public void newRevision(Object revision) {
        var anmeldung = SecurityContextHolder.getContext().getAuthentication();
        var benutzer = anmeldung == null
                        || anmeldung.getName() == null
                        || anmeldung.getName().isBlank()
                ? UNBEKANNT
                : anmeldung.getName();
        ((Revision) revision).erfassen(benutzer, MandantKontext.aktuell().orElse(null));
    }
}
