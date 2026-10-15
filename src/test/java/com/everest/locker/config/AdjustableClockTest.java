package com.everest.locker.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdjustableClockTest {

    private static final Instant START = Instant.parse("2026-01-01T09:00:00Z");

    @Test
    void standsStillUntilAdvanced() {
        var clock = new AdjustableClock(START);

        assertThat(clock.instant()).isEqualTo(START);
        assertThat(clock.instant()).isEqualTo(START);
    }

    @Test
    void movesForwardByTheGivenDuration() {
        var clock = new AdjustableClock(START);

        clock.advance(Duration.ofHours(30));

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-02T15:00:00Z"));
    }

    @Test
    void cannotGoBackwards() {
        var clock = new AdjustableClock(START);

        assertThatThrownBy(() -> clock.advance(Duration.ofHours(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
