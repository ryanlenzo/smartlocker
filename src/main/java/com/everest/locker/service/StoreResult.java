package com.everest.locker.service;

import com.everest.locker.entity.Size;

/**
 * What the delivery agent gets back after storing a package: where it went.
 * Deliberately has no pickup code; that goes only to the customer.
 */
public record StoreResult(String lockerId, Size lockerSize) {
}
