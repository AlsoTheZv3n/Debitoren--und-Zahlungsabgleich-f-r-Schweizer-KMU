package ch.kmu.debitoren.mahnwesen.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Grundkonfiguration jedes neuen Mandanten, wie in der Projektplanung vorgesehen: Erinnerung nach 10 Tagen ohne
 * Gebühr, 1. Mahnung nach 30 Tagen, 2. Mahnung nach 45 Tagen mit Gebühr. Admins passen sie danach an.
 */
public final class StandardMahnstufen {

    private StandardMahnstufen() {}

    public static List<Mahnstufendaten> daten() {
        return List.of(
                new Mahnstufendaten("Zahlungserinnerung", 10, 0, 10, BigDecimal.ZERO, """
                        Sicher ist es Ihnen im Alltag entgangen: Unsere Rechnung {rechnungsnummer} war am {faelligAm} \
                        fällig und ist noch offen.

                        Wir danken Ihnen, wenn Sie den offenen Betrag in den nächsten Tagen begleichen."""),
                new Mahnstufendaten("1. Mahnung", 30, 14, 10, BigDecimal.ZERO, """
                        Trotz unserer Zahlungserinnerung ist die Rechnung {rechnungsnummer} noch nicht bezahlt.

                        Bitte überweisen Sie den offenen Betrag von {offen} bis spätestens am {frist}."""),
                new Mahnstufendaten("2. Mahnung", 45, 14, 10, new BigDecimal("20.00"), """
                        Leider ist die Rechnung {rechnungsnummer} auch nach unserer Mahnung noch offen. Für den \
                        zusätzlichen Aufwand verrechnen wir eine Mahngebühr von {gebuehr}.

                        Bleibt die Zahlung bis am {frist} aus, leiten wir ohne weitere Mitteilung die Betreibung \
                        ein."""));
    }

    public static List<Mahnstufe> neueStufen() {
        var daten = daten();
        return IntStream.range(0, daten.size())
                .mapToObj(i -> new Mahnstufe(i + 1, daten.get(i)))
                .toList();
    }
}
