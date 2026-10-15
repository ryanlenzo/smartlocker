package com.everest.locker.support;

import com.everest.locker.config.AdjustableClock;
import com.everest.locker.hardware.LockerDoor;
import com.everest.locker.notification.Notifier;
import com.everest.locker.repository.InMemoryLockerRepository;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.strategy.ChargeTier;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import com.everest.locker.service.strategy.SmallestFitSelectionPolicy;
import com.everest.locker.service.strategy.StorageChargePolicy;
import com.everest.locker.service.strategy.TieredStorageChargePolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Test data builder: a LockerService with sensible fake collaborators, so each test only sets
 * the ones it cares about.
 */
public final class LockerServiceBuilder {

    public static final Instant START = Instant.parse("2026-01-01T09:00:00Z");

    /** The brief's example tiers with X = 1. */
    public static final StorageChargePolicy BRIEF_PRICING = new TieredStorageChargePolicy(BigDecimal.ONE, List.of(
            new ChargeTier(1, 5, BigDecimal.ONE),
            new ChargeTier(6, 10, BigDecimal.TWO),
            new ChargeTier(11, null, BigDecimal.valueOf(3))));

    private PickupCodeGenerator codes = new SequenceCodeGenerator("111111", "222222", "333333");
    private Notifier notifier = new RecordingNotifier();
    private LockerDoor door = new RecordingLockerDoor();
    private StorageChargePolicy chargePolicy = BRIEF_PRICING;
    private Clock clock = new AdjustableClock(START);

    private LockerServiceBuilder() {
    }

    public static LockerServiceBuilder aLockerService() {
        return new LockerServiceBuilder();
    }

    public LockerServiceBuilder withCodes(String... codes) {
        return withCodes(new SequenceCodeGenerator(codes));
    }

    public LockerServiceBuilder withCodes(PickupCodeGenerator codes) {
        this.codes = codes;
        return this;
    }

    public LockerServiceBuilder withNotifier(Notifier notifier) {
        this.notifier = notifier;
        return this;
    }

    public LockerServiceBuilder withDoor(LockerDoor door) {
        this.door = door;
        return this;
    }

    public LockerServiceBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public LockerService build() {
        return new LockerService(
                new InMemoryLockerRepository(),
                new SmallestFitSelectionPolicy(),
                codes,
                notifier,
                door,
                chargePolicy,
                clock);
    }
}
