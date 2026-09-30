package ch.kmu.debitoren.rechnung.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Speichert die Referenz ohne Leerzeichen; die Art ergibt sich beim Lesen aus dem Präfix. */
@Converter(autoApply = true)
public class ZahlungsreferenzConverter implements AttributeConverter<Zahlungsreferenz, String> {

    @Override
    public String convertToDatabaseColumn(Zahlungsreferenz referenz) {
        return referenz == null ? null : referenz.wert();
    }

    @Override
    public Zahlungsreferenz convertToEntityAttribute(String wert) {
        return wert == null ? null : Zahlungsreferenz.lesen(wert);
    }
}
