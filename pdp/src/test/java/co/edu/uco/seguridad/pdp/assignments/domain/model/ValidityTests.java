package co.edu.uco.seguridad.pdp.assignments.domain.model;

import co.edu.uco.seguridad.pdp.assignments.domain.exception.InvalidValidityException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidityTests {

    private static final Instant NOW = Instant.parse("2026-09-12T00:00:00Z");

    @Test
    void starting_now_has_no_end() {
        Validity validity = Validity.startingNow(NOW);

        assertThat(validity.validFrom()).isEqualTo(NOW);
        assertThat(validity.validUntil()).isEmpty();
    }

    @Test
    void ending_at_fixes_the_end_without_mutating_the_original() {
        Validity validity = Validity.startingNow(NOW);
        Instant later = NOW.plusSeconds(3600);

        Validity ended = validity.endingAt(later);

        assertThat(ended.validUntil()).contains(later);
        assertThat(validity.validUntil()).isEmpty();
    }

    @Test
    void is_not_active_before_it_starts() {
        Validity validity = Validity.startingNow(NOW);

        assertThat(validity.isActiveAt(NOW.minusSeconds(1))).isFalse();
    }

    @Test
    void without_an_end_is_always_active_after_it_starts() {
        Validity validity = Validity.startingNow(NOW);

        assertThat(validity.isActiveAt(NOW.plusSeconds(1_000_000))).isTrue();
    }

    @Test
    void is_active_before_a_future_end() {
        Validity validity = new Validity(NOW, Optional.of(NOW.plusSeconds(3600)));

        assertThat(validity.isActiveAt(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void is_not_active_after_a_past_end() {
        Validity validity = new Validity(NOW, Optional.of(NOW.plusSeconds(3600)));

        assertThat(validity.isActiveAt(NOW.plusSeconds(7200))).isFalse();
    }

    @Test
    void rejects_an_end_that_is_not_after_the_start() {
        assertThatThrownBy(() -> new Validity(NOW, Optional.of(NOW)))
                .isInstanceOf(InvalidValidityException.class);
    }
}
