package com.everest.locker.controller;

import com.everest.locker.notification.InMemoryInboxNotifier;
import com.everest.locker.notification.InboxMessage;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEMO ONLY ({@code --spring.profiles.active=demo}). Shows the messages "sent" to customers,
 * so the pickup flow can be tried without a real SMS provider. It does not exist in the
 * default profile because it exposes pickup codes.
 */
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoController {

    private final InMemoryInboxNotifier inbox;

    public DemoController(InMemoryInboxNotifier inbox) {
        this.inbox = inbox;
    }

    @GetMapping("/inbox")
    public List<InboxMessage> inbox() {
        return inbox.messages();
    }
}
