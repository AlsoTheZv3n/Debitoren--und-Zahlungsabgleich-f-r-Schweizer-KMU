package ch.kmu.debitoren.shared.web;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Eine Seite einer paginierten Liste; stabiler als das Format von Spring Data. */
public record Seite<T>(
        @NotNull List<T> inhalt,
        @NotNull int seite,
        @NotNull int groesse,
        @NotNull long anzahlElemente,
        @NotNull int anzahlSeiten) {

    public static <E, T> Seite<T> von(Page<E> seite, Function<? super E, T> abbildung) {
        return new Seite<>(
                seite.getContent().stream().<T>map(abbildung).toList(),
                seite.getNumber(),
                seite.getSize(),
                seite.getTotalElements(),
                seite.getTotalPages());
    }
}
