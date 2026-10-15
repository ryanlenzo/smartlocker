package com.everest.locker.support;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.PickupCode;
import com.everest.locker.notification.Notifier;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test fake: remembers every notification instead of sending it. */
public class RecordingNotifier implements Notifier {

    public record Sent(String customerContact, LockerId lockerId, PickupCode pickupCode) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendPickupCode(String customerContact, LockerId lockerId, PickupCode pickupCode) {
        sent.add(new Sent(customerContact, lockerId, pickupCode));
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }
}
