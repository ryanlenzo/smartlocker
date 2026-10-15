package com.everest.locker.notification;

/** A pickup-code message as the customer would receive it. Demo profile only. */
public record InboxMessage(String customerContact, String lockerId, String pickupCode) {
}
