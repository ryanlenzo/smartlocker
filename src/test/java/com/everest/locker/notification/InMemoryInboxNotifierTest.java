package com.everest.locker.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.PickupCode;
import org.junit.jupiter.api.Test;

class InMemoryInboxNotifierTest {

    private final InMemoryInboxNotifier inbox = new InMemoryInboxNotifier();

    @Test
    void keepsEverySentMessageInOrder() {
        inbox.sendPickupCode("+60111", new LockerId(1), new PickupCode("111111"));
        inbox.sendPickupCode("a@b.com", new LockerId(2), new PickupCode("222222"));

        assertThat(inbox.messages()).containsExactly(
                new InboxMessage("+60111", "L1", "111111"),
                new InboxMessage("a@b.com", "L2", "222222"));
    }

    @Test
    void isEmptyInitially() {
        assertThat(inbox.messages()).isEmpty();
    }
}
