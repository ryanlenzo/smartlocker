package com.everest.locker.controller;

import com.everest.locker.config.AdjustableClock;
import com.everest.locker.controller.dto.AdvanceClockRequest;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.notification.InMemoryInboxNotifier;
import com.everest.locker.notification.InboxMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEMO ONLY ({@code --spring.profiles.active=demo}). Shows the messages "sent" to customers and
 * moves the clock forward, so the pickup and storage-charge flows can be tried in minutes.
 * None of this exists in the default profile: the inbox exposes pickup codes.
 */
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoController {

    static final int MAX_HOURS = 24 * 365;

    private final InMemoryInboxNotifier inbox;
    private final AdjustableClock clock;

    public DemoController(InMemoryInboxNotifier inbox, AdjustableClock clock) {
        this.inbox = inbox;
        this.clock = clock;
    }

    public record ClockView(Instant now) {
    }

    @GetMapping("/inbox")
    public List<InboxMessage> inbox() {
        return inbox.messages();
    }

    @GetMapping("/clock")
    public ClockView clock() {
        return new ClockView(clock.instant());
    }

    @PostMapping("/clock/advance")
    public ClockView advanceClock(@RequestBody AdvanceClockRequest request) {
        Integer hours = request.hours();
        if (hours == null || hours < 1 || hours > MAX_HOURS) {
            throw new InvalidRequestException("Hours must be between 1 and " + MAX_HOURS + ".");
        }
        return new ClockView(clock.advance(Duration.ofHours(hours)));
    }
}
