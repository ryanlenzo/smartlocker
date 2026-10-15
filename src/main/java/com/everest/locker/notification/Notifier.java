package com.everest.locker.notification;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.PickupCode;

/**
 * The external notification system (SMS/email) that delivers pickup codes to customers.
 * Out of scope for the challenge, so only this interface and a stand-in implementation exist.
 */
@FunctionalInterface
public interface Notifier {

    /**
     * Sends the pickup code to the customer.
     *
     * @throws RuntimeException if the message could not be sent
     */
    void sendPickupCode(String customerContact, LockerId lockerId, PickupCode pickupCode);
}
