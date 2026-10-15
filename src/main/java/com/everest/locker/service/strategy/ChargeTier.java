package com.everest.locker.service.strategy;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One price band: days {@code fromDay} to {@code toDay} (inclusive) cost
 * {@code multiplier} x the unit rate per day. A {@code null toDay} means "and every day after".
 */
public record ChargeTier(int fromDay, Integer toDay, BigDecimal multiplier) {

    public ChargeTier {
        if (fromDay < 1) {
            throw new IllegalArgumentException("A tier must start on day 1 or later, was " + fromDay);
        }
        if (toDay != null && toDay < fromDay) {
            throw new IllegalArgumentException("A tier cannot end (day " + toDay + ") before it starts (day " + fromDay + ")");
        }
        Objects.requireNonNull(multiplier, "multiplier must not be null");
        if (multiplier.signum() < 0) {
            throw new IllegalArgumentException("A tier multiplier cannot be negative, was " + multiplier);
        }
    }

    boolean isOpenEnded() {
        return toDay == null;
    }

    /** How many of the first {@code totalDays} days fall inside this tier. */
    long daysWithin(long totalDays) {
        long lastDay = isOpenEnded() ? totalDays : Math.min(totalDays, toDay);
        return Math.max(0, lastDay - fromDay + 1);
    }
}
