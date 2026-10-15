package com.everest.locker.service;

import static com.everest.locker.entity.Size.SMALL;
import static com.everest.locker.support.ConcurrentRunner.runTogether;
import static com.everest.locker.support.LockerServiceBuilder.aLockerService;
import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.entity.LockerStatus;
import com.everest.locker.entity.Size;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.service.strategy.RandomPickupCodeGenerator;
import com.everest.locker.support.ConcurrentRunner.Outcome;
import com.everest.locker.support.RecordingNotifier;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Level 4: many delivery agents storing packages at the same moment against the real service
 * (random codes, in-memory repository, smallest-fit selection).
 */
class ConcurrentStorageTest {

    private static final int LOCKERS = 10;
    private static final int AGENTS = 50;

    private final RecordingNotifier notifier = new RecordingNotifier();
    private final LockerService service = aLockerService()
            .withCodes(new RandomPickupCodeGenerator(new SecureRandom()))
            .withNotifier(notifier)
            .build();

    @RepeatedTest(50)
    void moreAgentsThanLockersFillEveryLockerExactlyOnceAndTurnTheRestAway() throws Exception {
        service.addLockers(SMALL, LOCKERS);

        List<Outcome<StoreResult>> outcomes = runTogether(AGENTS, () -> service.storeParcel(SMALL, "+60123456789"));

        List<String> assignedLockers = outcomes.stream()
                .filter(Outcome::succeeded)
                .map(outcome -> outcome.result().lockerId())
                .toList();
        assertThat(assignedLockers).hasSize(LOCKERS).doesNotHaveDuplicates();
        assertThat(outcomes)
                .filteredOn(outcome -> !outcome.succeeded())
                .hasSize(AGENTS - LOCKERS)
                .allMatch(outcome -> outcome.failure() instanceof NoSuitableLockerException);
        assertThat(service.listLockers()).allMatch(view -> view.status() == LockerStatus.OCCUPIED);
        assertThat(notifier.sent()).hasSize(LOCKERS);
    }

    @RepeatedTest(50)
    void packagesStoredAtTheSameTimeNeverShareAPickupCode() throws Exception {
        service.addLockers(SMALL, AGENTS);

        runTogether(AGENTS, () -> service.storeParcel(SMALL, "+60123456789"));

        assertThat(notifier.sent())
                .hasSize(AGENTS)
                .extracting(sent -> sent.pickupCode().value())
                .doesNotHaveDuplicates();
    }

    @RepeatedTest(20)
    void concurrentStoresStillPreferTheSmallestLockers() throws Exception {
        service.addLockers(Size.LARGE, 5);   // L1-L5
        service.addLockers(SMALL, 5);        // L6-L10

        List<Outcome<StoreResult>> outcomes = runTogether(5, () -> service.storeParcel(SMALL, "+60123456789"));

        assertThat(outcomes)
                .allMatch(Outcome::succeeded)
                .extracting(outcome -> outcome.result().lockerSize())
                .containsOnly(SMALL);
    }

    @RepeatedTest(20)
    void storingAndCollectingAtTheSameTimeKeepsAvailabilityCorrect() throws Exception {
        service.addLockers(SMALL, LOCKERS);
        for (int i = 0; i < LOCKERS; i++) {
            service.storeParcel(SMALL, "+60123456789");
        }
        List<RecordingNotifier.Sent> firstBatch = notifier.sent();

        // Every customer collects while the same number of new agents try to store.
        AtomicInteger turn = new AtomicInteger();
        List<Outcome<Object>> outcomes = runTogether(2 * LOCKERS, () -> {
            int me = turn.getAndIncrement();
            if (me < LOCKERS) {
                RecordingNotifier.Sent sent = firstBatch.get(me);
                return service.retrieveParcel(sent.lockerId().toString(), sent.pickupCode().value());
            }
            return service.storeParcel(SMALL, "+60123456789");
        });

        long collected = outcomes.stream().filter(o -> o.result() instanceof RetrievalResult).count();
        long stored = outcomes.stream().filter(o -> o.result() instanceof StoreResult).count();
        long occupied = service.listLockers().stream().filter(view -> view.status() == LockerStatus.OCCUPIED).count();

        assertThat(collected).isEqualTo(LOCKERS);
        assertThat(occupied).isEqualTo(LOCKERS - collected + stored);
        assertThat(outcomes)
                .filteredOn(outcome -> !outcome.succeeded())
                .allMatch(outcome -> outcome.failure() instanceof NoSuitableLockerException);
    }
}
