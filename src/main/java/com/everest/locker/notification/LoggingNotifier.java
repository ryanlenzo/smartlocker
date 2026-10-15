package com.everest.locker.notification;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.PickupCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in for the external SMS/email system, which is out of scope. It writes the message to
 * the application log so the flow can be followed locally. A real adapter would call the
 * provider's API instead and must never log the code.
 */
public class LoggingNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

    @Override
    public void sendPickupCode(String customerContact, LockerId lockerId, PickupCode pickupCode) {
        log.info("[simulated SMS to {}] Your package is in locker {}. Pickup code: {}",
                customerContact, lockerId, pickupCode.value());
    }
}
