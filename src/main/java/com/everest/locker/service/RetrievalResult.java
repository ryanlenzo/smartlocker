package com.everest.locker.service;

import com.everest.locker.entity.Size;

/** Pickup confirmation for the customer: which locker opened. */
public record RetrievalResult(String lockerId, Size lockerSize) {
}
