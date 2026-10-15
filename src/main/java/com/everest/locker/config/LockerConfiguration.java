package com.everest.locker.config;

import com.everest.locker.hardware.LockerDoor;
import com.everest.locker.hardware.LoggingLockerDoor;
import com.everest.locker.notification.InMemoryInboxNotifier;
import com.everest.locker.notification.LoggingNotifier;
import com.everest.locker.notification.Notifier;
import com.everest.locker.repository.InMemoryLockerRepository;
import com.everest.locker.repository.LockerRepository;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.strategy.LockerSelectionPolicy;
import com.everest.locker.service.strategy.PickupCodeGenerator;
import com.everest.locker.service.strategy.RandomPickupCodeGenerator;
import com.everest.locker.service.strategy.SmallestFitSelectionPolicy;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Composition root: the only place that picks concrete implementations. The core classes have
 * no Spring annotations; swapping an implementation (e.g. a database repository or a real SMS
 * notifier) is a change here only.
 */
@Configuration
public class LockerConfiguration {

    @Bean
    LockerRepository lockerRepository() {
        return new InMemoryLockerRepository();
    }

    @Bean
    LockerSelectionPolicy lockerSelectionPolicy() {
        return new SmallestFitSelectionPolicy();
    }

    @Bean
    PickupCodeGenerator pickupCodeGenerator() {
        return new RandomPickupCodeGenerator(new SecureRandom());
    }

    @Bean
    @Profile("!demo")
    Notifier notifier() {
        return new LoggingNotifier();
    }

    /** Demo profile: keep messages in an inbox the demo endpoint can show, instead of logging them. */
    @Bean
    @Profile("demo")
    InMemoryInboxNotifier inboxNotifier() {
        return new InMemoryInboxNotifier();
    }

    @Bean
    LockerDoor lockerDoor() {
        return new LoggingLockerDoor();
    }

    @Bean
    LockerService lockerService(LockerRepository lockerRepository,
                                LockerSelectionPolicy lockerSelectionPolicy,
                                PickupCodeGenerator pickupCodeGenerator,
                                Notifier notifier,
                                LockerDoor lockerDoor) {
        return new LockerService(
                lockerRepository, lockerSelectionPolicy, pickupCodeGenerator, notifier, lockerDoor);
    }
}
