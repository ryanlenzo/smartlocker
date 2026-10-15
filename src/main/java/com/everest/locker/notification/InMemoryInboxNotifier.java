package com.everest.locker.notification;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.PickupCode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DEMO ONLY. Keeps every "sent" message in memory so a demo (or the UI) can show what the
 * customer received. Never wire this in production: it exposes pickup codes.
 */
public class InMemoryInboxNotifier implements Notifier {

    private final List<InboxMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void sendPickupCode(String customerContact, LockerId lockerId, PickupCode pickupCode) {
        messages.add(new InboxMessage(customerContact, lockerId.toString(), pickupCode.value()));
    }

    /** Every message sent so far, oldest first. */
    public List<InboxMessage> messages() {
        return List.copyOf(messages);
    }
}
