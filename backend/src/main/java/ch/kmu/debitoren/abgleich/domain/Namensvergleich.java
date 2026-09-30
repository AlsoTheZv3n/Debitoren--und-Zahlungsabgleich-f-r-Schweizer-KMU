package ch.kmu.debitoren.abgleich.domain;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Vergleicht den Namen auf der Rechnung mit dem Auftraggeber aus der Bankdatei. Banken kürzen, schreiben
 * gross oder drehen Vor- und Nachname; Rechtsformen wie AG oder GmbH fehlen oft. Deshalb zählen die Wörter,
 * nicht die Schreibweise.
 */
public final class Namensvergleich {

    private static final Set<String> RECHTSFORMEN = Set.of(
            "ag",
            "gmbh",
            "sa",
            "sarl",
            "sagl",
            "kg",
            "klg",
            "co",
            "cie",
            "und",
            "inc",
            "ltd",
            "llc",
            "se",
            "genossenschaft",
            "stiftung",
            "verein",
            "einzelfirma",
            "herr",
            "frau",
            "familie",
            "fam");

    private Namensvergleich() {}

    /**
     * Passen, wenn jedes Wort des einen Namens im anderen vorkommt; ein einzelner Buchstabe gilt als Initiale.
     * Mindestens ein ganzes Wort muss übereinstimmen.
     */
    public static boolean passen(String rechnungsname, String auftraggeber) {
        var a = woerter(rechnungsname);
        var b = woerter(auftraggeber);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return deckt(a, b) || deckt(b, a);
    }

    /** Jedes Wort aus {@code teil} steht in {@code ganz}, als ganzes Wort oder als Initiale. */
    private static boolean deckt(List<String> teil, List<String> ganz) {
        boolean ganzesWort = false;
        for (var wort : teil) {
            if (ganz.contains(wort)) {
                ganzesWort |= wort.length() > 1;
            } else if (wort.length() != 1 || ganz.stream().noneMatch(anderes -> anderes.startsWith(wort))) {
                return false;
            }
        }
        return ganzesWort;
    }

    static List<String> woerter(String name) {
        if (name == null) {
            return List.of();
        }
        var ohneUmlaute = name.toLowerCase(Locale.ROOT)
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");
        var ohneAkzente = Normalizer.normalize(ohneUmlaute, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(ohneAkzente.split("[^a-z0-9]+"))
                .filter(wort -> !wort.isEmpty())
                .filter(wort -> !RECHTSFORMEN.contains(wort))
                .distinct()
                .toList();
    }
}
