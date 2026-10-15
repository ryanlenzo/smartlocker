package com.everest.locker.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TieredStorageChargePolicyTest {

    /** The brief's example: X/day for days 1-5, 2X for days 6-10, 3X after that. */
    private static final List<ChargeTier> BRIEF_TIERS = List.of(
            new ChargeTier(1, 5, BigDecimal.ONE),
            new ChargeTier(6, 10, BigDecimal.TWO),
            new ChargeTier(11, null, BigDecimal.valueOf(3)));

    private final TieredStorageChargePolicy policy = new TieredStorageChargePolicy(BigDecimal.ONE, BRIEF_TIERS);

    @ParameterizedTest(name = "stored {0} -> {1} day(s), charge {2}")
    @CsvSource({
            "PT0S,           1,  1.00",
            "PT23H59M59S,    1,  1.00",
            "PT24H,          1,  1.00",
            "PT24H0M1S,      2,  2.00",
            "P5D,            5,  5.00",
            "P5DT1S,         6,  7.00",
            "P10D,          10, 15.00",
            "P10DT1S,       11, 18.00",
            "P12D,          12, 21.00"
    })
    void chargesEachStartedDayAtItsTierRate(Duration storedFor, long expectedDays, BigDecimal expectedCharge) {
        StorageCharge charge = policy.chargeFor(storedFor);

        assertThat(charge.chargeableDays()).isEqualTo(expectedDays);
        assertThat(charge.amount()).isEqualByComparingTo(expectedCharge);
    }

    @Test
    void scalesWithTheUnitRate() {
        var policy = new TieredStorageChargePolicy(new BigDecimal("2.50"), BRIEF_TIERS);

        assertThat(policy.chargeFor(Duration.ofDays(12)).amount()).isEqualByComparingTo("52.50");
    }

    @Test
    void amountsAlwaysHaveTwoDecimalPlaces() {
        assertThat(policy.chargeFor(Duration.ofDays(3)).amount()).hasToString("3.00");
    }

    @Test
    void aClockGoingBackwardsStillCountsAsOneDay() {
        assertThat(policy.chargeFor(Duration.ofHours(-2)).chargeableDays()).isEqualTo(1);
    }

    @Test
    void aFreeGracePeriodIsJustATierWithMultiplierZero() {
        var withGracePeriod = new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(1, 2, BigDecimal.ZERO),
                new ChargeTier(3, null, BigDecimal.ONE)));

        assertThat(withGracePeriod.chargeFor(Duration.ofDays(2)).amount()).isEqualByComparingTo("0");
        assertThat(withGracePeriod.chargeFor(Duration.ofDays(4)).amount()).isEqualByComparingTo("2");
    }

    @Test
    void rejectsANegativeUnitRate() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(new BigDecimal("-1"), BRIEF_TIERS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit rate");
    }

    @Test
    void rejectsNoTiers() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tiersMustStartOnDayOne() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(2, null, BigDecimal.ONE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day 1");
    }

    @Test
    void tiersMustNotLeaveGaps() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(1, 5, BigDecimal.ONE),
                new ChargeTier(7, null, BigDecimal.TWO))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day 6");
    }

    @Test
    void tiersMustNotOverlap() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(1, 5, BigDecimal.ONE),
                new ChargeTier(5, null, BigDecimal.TWO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theLastTierMustBeOpenEnded() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(1, 5, BigDecimal.ONE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last tier");
    }

    @Test
    void onlyTheLastTierMayBeOpenEnded() {
        assertThatThrownBy(() -> new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
                new ChargeTier(1, null, BigDecimal.ONE),
                new ChargeTier(6, null, BigDecimal.TWO))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTierCannotEndBeforeItStarts() {
        assertThatThrownBy(() -> new ChargeTier(6, 5, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTierCannotHaveANegativeMultiplier() {
        assertThatThrownBy(() -> new ChargeTier(1, null, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
