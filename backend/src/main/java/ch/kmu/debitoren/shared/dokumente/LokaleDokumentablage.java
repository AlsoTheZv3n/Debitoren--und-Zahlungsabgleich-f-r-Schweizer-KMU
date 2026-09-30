package ch.kmu.debitoren.shared.dokumente;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ablage im lokalen Dateisystem für Entwicklung und Tests. Dateien werden erst unter einem temporären
 * Namen geschrieben und dann per Hardlink an ihren Platz gebracht, der nie eine bestehende Datei ersetzt.
 */
@Component
@ConditionalOnProperty(name = "debitoren.dokumente.ablage", havingValue = "lokal", matchIfMissing = true)
class LokaleDokumentablage implements Dokumentablage {

    private static final Pattern GUELTIGER_SCHLUESSEL =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*(/[A-Za-z0-9._-]+)*");

    private final Path wurzel;

    LokaleDokumentablage(@Value("${debitoren.dokumente.verzeichnis:data/dokumente}") Path verzeichnis) {
        this.wurzel = verzeichnis.toAbsolutePath().normalize();
    }

    @Override
    public void ablegen(String schluessel, byte[] inhalt) {
        var ziel = pfad(schluessel);
        try {
            Files.createDirectories(ziel.getParent());
            var temporaer = Files.createTempFile(ziel.getParent(), ".neu-", ".tmp");
            try {
                Files.write(temporaer, inhalt);
                // Ein Hardlink scheitert, wenn das Ziel existiert; ein Verschieben würde es je nach System ersetzen.
                Files.createLink(ziel, temporaer);
            } catch (FileAlreadyExistsException e) {
                throw new DokumentBereitsAbgelegt(schluessel);
            } finally {
                Files.deleteIfExists(temporaer);
            }
            if (!ziel.toFile().setReadOnly()) {
                throw new IOException("Schreibschutz nicht setzbar: " + ziel);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<byte[]> lesen(String schluessel) {
        var quelle = pfad(schluessel);
        if (!Files.isRegularFile(quelle)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(quelle));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path pfad(String schluessel) {
        if (schluessel == null || !GUELTIGER_SCHLUESSEL.matcher(schluessel).matches() || schluessel.contains("..")) {
            throw new IllegalArgumentException("Ungültiger Dokumentschlüssel: " + schluessel);
        }
        var pfad = wurzel.resolve(schluessel).normalize();
        if (!pfad.startsWith(wurzel)) {
            throw new IllegalArgumentException("Dokumentschlüssel ausserhalb der Ablage: " + schluessel);
        }
        return pfad;
    }
}
