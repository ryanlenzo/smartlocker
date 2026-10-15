package com.everest.locker.service.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Charges per started day, with the daily price depending on which tier the day falls in.
 * The brief's example is X/day for days 1-5, 2X for days 6-10 and 3X after that; the rate and
 * tiers are configuration, so a free grace period is simply a tier with multiplier 0.
 *
 * <p>A day is a 24-hour block counted from when the package was stored, and any started block
 * counts as a full day, so every stay costs at least one day.
 */
public class TieredStorageChargePolicy implements StorageChargePolicy {

    private static final Duration ONE_DAY = Duration.ofHours(24);

    private final BigDecimal unitRate;
    private final List<ChargeTier> tiers;

    public TieredStorageChargePolicy(BigDecimal unitRate, List<ChargeTier> tiers) {
        Objects.requireNonNull(unitRate, "unit rate must not be null");
        if (unitRate.signum() < 0) {
            throw new IllegalArgumentException("The unit rate cannot be negative, was " + unitRate);
        }
        this.unitRate = unitRate;
        this.tiers = List.copyOf(Objects.requireNonNull(tiers, "tiers must not be null"));
        requireTiersCoverEveryDay(this.tiers);
    }

    @Override
    public StorageCharge chargeFor(Duration storedFor) {
        long days = chargeableDays(storedFor);
        BigDecimal amount = BigDecimal.ZERO;
        for (ChargeTier tier : tiers) {
            BigDecimal dailyPrice = unitRate.multiply(tier.multiplier());
            amount = amount.add(dailyPrice.multiply(BigDecimal.valueOf(tier.daysWithin(days))));
        }
        return new StorageCharge(days, amount.setScale(2, RoundingMode.HALF_UP));
    }

    /** Started 24-hour blocks, minimum 1: 24h is 1 day, 24h + 1s is 2 days. */
    static long chargeableDays(Duration storedFor) {
        if (storedFor.isNegative() || storedFor.isZero()) {
            return 1;
        }
        long fullDays = storedFor.dividedBy(ONE_DAY);
        boolean partOfAnotherDay = storedFor.compareTo(ONE_DAY.multipliedBy(fullDays)) > 0;
        return partOfAnotherDay ? fullDays + 1 : fullDays;
    }

    /** Tiers must run from day 1 with no gaps or overlaps, and the last one must never end. */
    private static void requireTiersCoverEveryDay(List<ChargeTier> tiers) {
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("At least one charge tier is required");
        }
        int expectedStart = 1;
        for (int i = 0; i < tiers.size(); i++) {
            ChargeTier tier = tiers.get(i);
            if (tier.fromDay() != expectedStart) {
                throw new IllegalArgumentException(
                        "Charge tiers must be continuous from day 1: expected a tier starting on day "
                                + expectedStart + " but found day " + tier.fromDay());
            }
            boolean isLast = i == tiers.size() - 1;
            if (tier.isOpenEnded() != isLast) {
                throw new IllegalArgumentException("Only the last tier must be open-ended (no end day)");
            }
            if (!isLast) {
                expectedStart = tier.toDay() + 1;
            }
        }
    }
}
