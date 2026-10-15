package com.everest.locker.service.strategy;

import static com.everest.locker.entity.Size.LARGE;
import static com.everest.locker.entity.Size.MEDIUM;
import static com.everest.locker.entity.Size.SMALL;
import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.Parcel;
import com.everest.locker.entity.PickupCode;
import com.everest.locker.entity.Size;
import com.everest.locker.entity.StoredParcel;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmallestFitSelectionPolicyTest {

    private final SmallestFitSelectionPolicy policy = new SmallestFitSelectionPolicy();

    @Test
    void prefersTheSmallestLockerThatFits() {
        List<Locker> lockers = List.of(locker(1, LARGE), locker(2, MEDIUM), locker(3, SMALL));

        assertThat(ids(policy.candidates(lockers, SMALL))).containsExactly(3, 2, 1);
    }

    @Test
    void fallsBackToABiggerLockerWhenNoExactSizeIsFree() {
        List<Locker> lockers = List.of(locker(1, LARGE), occupied(locker(2, MEDIUM)));

        assertThat(ids(policy.candidates(lockers, MEDIUM))).containsExactly(1);
    }

    @Test
    void neverOffersALockerThatIsTooSmall() {
        List<Locker> lockers = List.of(locker(1, SMALL), locker(2, MEDIUM));

        assertThat(policy.candidates(lockers, LARGE)).isEmpty();
    }

    @Test
    void skipsOccupiedLockers() {
        List<Locker> lockers = List.of(occupied(locker(1, SMALL)), locker(2, SMALL));

        assertThat(ids(policy.candidates(lockers, SMALL))).containsExactly(2);
    }

    @Test
    void breaksTiesBetweenSameSizeLockersByLowestNumber() {
        List<Locker> lockers = List.of(locker(10, SMALL), locker(2, SMALL), locker(5, SMALL));

        assertThat(ids(policy.candidates(lockers, SMALL))).containsExactly(2, 5, 10);
    }

    @Test
    void returnsNothingWhenThereAreNoLockers() {
        assertThat(policy.candidates(List.of(), SMALL)).isEmpty();
    }

    private static Locker locker(int number, Size size) {
        return new Locker(new LockerId(number), size);
    }

    private static Locker occupied(Locker locker) {
        locker.tryOccupy(new StoredParcel(new Parcel(SMALL, "a@b.com"), new PickupCode("000001")));
        return locker;
    }

    private static List<Integer> ids(List<Locker> lockers) {
        return lockers.stream().map(locker -> locker.id().number()).toList();
    }
}
