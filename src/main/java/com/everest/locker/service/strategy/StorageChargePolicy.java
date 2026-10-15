package com.everest.locker.service.strategy;

import java.time.Duration;

/** Decides what a customer pays for the time their package spent in a locker. */
@FunctionalInterface
public interface StorageChargePolicy {

    StorageCharge chargeFor(Duration storedFor);
}
