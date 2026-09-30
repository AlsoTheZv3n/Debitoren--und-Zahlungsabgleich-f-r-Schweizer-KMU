package ch.kmu.debitoren.shared.nummern;

import ch.kmu.debitoren.shared.mandant.MandantKontext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vergibt fortlaufende Nummern je Mandant, Art und Jahr, etwa für Rechnungs- und Kundennummern.
 *
 * <p>Die Zeile des Nummernkreises bleibt bis zum Ende der Transaktion gesperrt. Parallele Vergaben warten
 * also aufeinander, und bricht die Transaktion ab, ist auch die Nummer wieder frei: es entstehen keine
 * Lücken.
 */
@Component
public class Nummernvergabe {

    private final JdbcClient jdbc;

    Nummernvergabe(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long naechste(String art, int jahr) {
        return jdbc.sql("""
                        INSERT INTO nummernkreis (mandant_id, art, jahr, letzte_nummer)
                        VALUES (:mandant, :art, :jahr, 1)
                        ON CONFLICT (mandant_id, art, jahr)
                        DO UPDATE SET letzte_nummer = nummernkreis.letzte_nummer + 1
                        RETURNING letzte_nummer
                        """)
                .param("mandant", MandantKontext.erforderlich())
                .param("art", art)
                .param("jahr", jahr)
                .query(Long.class)
                .single();
    }
}
