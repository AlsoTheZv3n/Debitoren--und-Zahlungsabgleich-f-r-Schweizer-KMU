package ch.kmu.debitoren.shared.dokumente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LokaleDokumentablageTest {

    @TempDir
    Path verzeichnis;

    @Test
    void legtAbUndLiestWieder() {
        var ablage = new LokaleDokumentablage(verzeichnis);
        ablage.ablegen("rechnungen/m1/r1.pdf", bytes("PDF"));

        assertThat(ablage.lesen("rechnungen/m1/r1.pdf"))
                .hasValueSatisfying(inhalt -> assertThat(inhalt).isEqualTo(bytes("PDF")));
        assertThat(ablage.lesen("rechnungen/m1/fehlt.pdf")).isEmpty();
        assertThat(Files.isWritable(verzeichnis.resolve("rechnungen/m1/r1.pdf")))
                .isFalse();
    }

    @Test
    void ueberschreibtNie() {
        var ablage = new LokaleDokumentablage(verzeichnis);
        ablage.ablegen("a/b.pdf", bytes("erste Fassung"));

        assertThatThrownBy(() -> ablage.ablegen("a/b.pdf", bytes("zweite Fassung")))
                .isInstanceOf(DokumentBereitsAbgelegt.class);
        assertThat(ablage.lesen("a/b.pdf")).hasValue(bytes("erste Fassung"));
        try (var dateien = Files.list(verzeichnis.resolve("a"))) {
            assertThat(dateien).as("keine temporären Reste").hasSize(1);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"../ausserhalb.pdf", "/absolut.pdf", "a/../../b.pdf", "a//b.pdf", "a b.pdf", ""})
    void verweigertSchluesselAusserhalbDerAblage(String schluessel) {
        var ablage = new LokaleDokumentablage(verzeichnis);
        assertThatThrownBy(() -> ablage.ablegen(schluessel, bytes("x"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
