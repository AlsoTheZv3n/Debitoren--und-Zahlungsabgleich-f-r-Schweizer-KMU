package ch.kmu.debitoren.shared.mandant;

import java.util.Map;
import java.util.UUID;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Liefert Hibernate den Mandanten für {@code @TenantId}. Hibernate filtert damit jede Abfrage und setzt
 * {@code mandant_id} beim Einfügen.
 */
@Component
public class MandantTenantResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /** Steht für "kein Mandant gewählt": trifft keine Zeile, und Einfügen scheitert am Fremdschlüssel. */
    static final UUID KEIN_MANDANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return MandantKontext.aktuell().orElse(KEIN_MANDANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> eigenschaften) {
        eigenschaften.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
