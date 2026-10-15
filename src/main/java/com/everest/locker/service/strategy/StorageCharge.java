package com.everest.locker.service.strategy;

import java.math.BigDecimal;

/** The result of pricing a stay: how many days were charged and the total amount. */
public record StorageCharge(long chargeableDays, BigDecimal amount) {
}
