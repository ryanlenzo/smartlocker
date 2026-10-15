package com.everest.locker.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LockerTest {

    private final Locker locker = new Locker(new LockerId(1), Size.MEDIUM);
    private final StoredParcel parcel = storedParcel("123456");

    @Test
    void newLockerIsAvailable() {
        assertThat(locker.isAvailable()).isTrue();
        assertThat(locker.status()).isEqualTo(LockerStatus.AVAILABLE);
        assertThat(locker.occupant()).isEmpty();
    }

    @Test
    void occupyingAnEmptyLockerSucceeds() {
        assertThat(locker.tryOccupy(parcel)).isTrue();

        assertThat(locker.isAvailable()).isFalse();
        assertThat(locker.status()).isEqualTo(LockerStatus.OCCUPIED);
        assertThat(locker.occupant()).contains(parcel);
    }

    @Test
    void occupiedLockerRefusesASecondParcelAndKeepsTheFirst() {
        locker.tryOccupy(parcel);

        assertThat(locker.tryOccupy(storedParcel("654321"))).isFalse();
        assertThat(locker.occupant()).contains(parcel);
    }

    @Test
    void releaseEmptiesTheLockerAndReturnsTheParcel() {
        locker.tryOccupy(parcel);

        assertThat(locker.release()).isEqualTo(parcel);
        assertThat(locker.isAvailable()).isTrue();
    }

    @Test
    void releasingAnEmptyLockerFails() {
        assertThatThrownBy(locker::release).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theRightCodeReleasesTheParcel() {
        locker.tryOccupy(parcel);

        assertThat(locker.releaseIfCodeMatches(new PickupCode("123456"))).contains(parcel);
        assertThat(locker.isAvailable()).isTrue();
    }

    @Test
    void aWrongCodeReleasesNothing() {
        locker.tryOccupy(parcel);

        assertThat(locker.releaseIfCodeMatches(new PickupCode("000000"))).isEmpty();
        assertThat(locker.occupant()).contains(parcel);
    }

    @Test
    void anEmptyLockerReleasesNothing() {
        assertThat(locker.releaseIfCodeMatches(new PickupCode("123456"))).isEmpty();
    }

    @Test
    void cannotOccupyWithNull() {
        assertThatThrownBy(() -> locker.tryOccupy(null)).isInstanceOf(NullPointerException.class);
    }

    private static StoredParcel storedParcel(String code) {
        return new StoredParcel(new Parcel(Size.SMALL, "+60123456789"), new PickupCode(code));
    }
}
