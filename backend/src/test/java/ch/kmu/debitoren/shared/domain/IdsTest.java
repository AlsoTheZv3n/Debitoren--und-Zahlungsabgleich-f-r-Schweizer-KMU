package ch.kmu.debitoren.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void erzeugtUuidVersion7MitRfcVariante() {
        var id = Ids.neu();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void enthaeltDenZeitstempelInMillisekunden() {
        long vorher = System.currentTimeMillis();
        long zeit = Ids.neu().getMostSignificantBits() >>> 16;
        long nachher = System.currentTimeMillis();
        assertThat(zeit).isBetween(vorher, nachher);
    }

    @Test
    void istEindeutig() {
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(Ids.neu());
        }
        assertThat(ids).hasSize(10_000);
    }
}
