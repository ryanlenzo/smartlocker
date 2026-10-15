package com.everest.locker.service;

import com.everest.locker.entity.Size;
import java.time.Instant;

/**
 * What the delivery agent gets back after storing a package: where it went and when.
 * Deliberately has no pickup code; that goes only to the customer.
 */
public record StoreResult(String lockerId, Size lockerSize, Instant storedAt) {
}
