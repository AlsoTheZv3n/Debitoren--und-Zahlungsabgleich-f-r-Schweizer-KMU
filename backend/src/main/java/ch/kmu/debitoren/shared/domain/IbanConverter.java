package ch.kmu.debitoren.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Speichert eine {@link Iban} als Text ohne Leerzeichen. */
@Converter(autoApply = true)
public class IbanConverter implements AttributeConverter<Iban, String> {

    @Override
    public String convertToDatabaseColumn(Iban iban) {
        return iban == null ? null : iban.wert();
    }

    @Override
    public Iban convertToEntityAttribute(String wert) {
        return wert == null ? null : new Iban(wert);
    }
}
