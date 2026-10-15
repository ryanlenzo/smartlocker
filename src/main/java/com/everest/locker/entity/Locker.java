package com.everest.locker.entity;

import java.util.Objects;
import java.util.Optional;

/**
 * A physical locker. It owns its occupancy state: callers can only try to occupy it or
 * release it, never set its contents directly, so "one package at a time" is enforced here.
 */
public final class Locker {

    private final LockerId id;
    private final Size size;
    private StoredParcel occupant;

    public Locker(LockerId id, Size size) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.size = Objects.requireNonNull(size, "size must not be null");
    }

    /**
     * Puts the parcel in this locker if it is empty.
     *
     * @return {@code true} if the parcel was stored, {@code false} if the locker was already occupied
     */
    public boolean tryOccupy(StoredParcel parcel) {
        Objects.requireNonNull(parcel, "parcel must not be null");
        if (occupant != null) {
            return false;
        }
        occupant = parcel;
        return true;
    }

    /**
     * Empties the locker.
     *
     * @return the parcel that was inside
     * @throws IllegalStateException if the locker is already empty
     */
    public StoredParcel release() {
        if (occupant == null) {
            throw new IllegalStateException("Locker " + id + " is already empty");
        }
        StoredParcel released = occupant;
        occupant = null;
        return released;
    }

    /**
     * Empties the locker only if the given code belongs to the parcel inside.
     *
     * @return the released parcel, or empty if the locker is empty or the code does not match;
     *         in that case nothing changes
     */
    public Optional<StoredParcel> releaseIfCodeMatches(PickupCode code) {
        Objects.requireNonNull(code, "code must not be null");
        if (occupant == null || !occupant.pickupCode().equals(code)) {
            return Optional.empty();
        }
        return Optional.of(release());
    }

    public LockerId id() {
        return id;
    }

    public Size size() {
        return size;
    }

    public boolean isAvailable() {
        return occupant == null;
    }

    public LockerStatus status() {
        return isAvailable() ? LockerStatus.AVAILABLE : LockerStatus.OCCUPIED;
    }

    public Optional<StoredParcel> occupant() {
        return Optional.ofNullable(occupant);
    }
}
