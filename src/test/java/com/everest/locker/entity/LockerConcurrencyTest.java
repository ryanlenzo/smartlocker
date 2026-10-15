package com.everest.locker.entity;

import static com.everest.locker.support.ConcurrentRunner.runTogether;
import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.support.ConcurrentRunner.Outcome;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;

/** Level 4 at the lowest level: one locker, many threads, at most one winner. */
class LockerConcurrencyTest {

    private static final int THREADS = 32;

    @RepeatedTest(50)
    void manyThreadsRacingForOneLockerProduceExactlyOneWinner() throws Exception {
        Locker locker = new Locker(new LockerId(1), Size.SMALL);
        AtomicInteger next = new AtomicInteger();

        List<Outcome<StoredParcel>> outcomes = runTogether(THREADS, () -> {
            StoredParcel mine = parcel("%06d".formatted(next.incrementAndGet()));
            return locker.tryOccupy(mine) ? mine : null;
        });

        List<StoredParcel> winners = outcomes.stream()
                .map(Outcome::result)
                .filter(result -> result != null)
                .toList();
        assertThat(winners).hasSize(1);
        assertThat(locker.occupant()).contains(winners.getFirst());
    }

    @RepeatedTest(50)
    void theSameCodeUsedManyTimesAtOnceReleasesTheParcelOnlyOnce() throws Exception {
        Locker locker = new Locker(new LockerId(1), Size.SMALL);
        locker.tryOccupy(parcel("123456"));

        List<Outcome<Optional<StoredParcel>>> outcomes =
                runTogether(THREADS, () -> locker.releaseIfCodeMatches(new PickupCode("123456")));

        assertThat(outcomes).allMatch(Outcome::succeeded);
        assertThat(outcomes).filteredOn(outcome -> outcome.result().isPresent()).hasSize(1);
        assertThat(locker.isAvailable()).isTrue();
    }

    private static StoredParcel parcel(String code) {
        return new StoredParcel(new Parcel(Size.SMALL, "+60123456789"), new PickupCode(code), Instant.EPOCH);
    }
}
