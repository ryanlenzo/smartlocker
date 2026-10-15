package com.everest.locker.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A clock that stands still until it is moved forward. Used by the demo profile to show storage
 * charges without waiting days, and by tests to control time precisely.
 */
public class AdjustableClock extends Clock {

    private final AtomicReference<Instant> now;

    public AdjustableClock(Instant start) {
        this.now = new AtomicReference<>(Objects.requireNonNull(start, "start must not be null"));
    }

    public Instant advance(Duration amount) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("The clock can only move forward");
        }
        return now.updateAndGet(current -> current.plus(amount));
    }

    @Override
    public Instant instant() {
        return now.get();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("AdjustableClock always uses UTC");
    }
}
