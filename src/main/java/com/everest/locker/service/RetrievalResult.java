package com.everest.locker.service;

import com.everest.locker.entity.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Pickup confirmation for the customer: which locker opened and what the stay costs. */
public record RetrievalResult(String lockerId,
                              Size lockerSize,
                              Instant storedAt,
                              Instant retrievedAt,
                              long chargeableDays,
                              BigDecimal storageCharge) {
}
