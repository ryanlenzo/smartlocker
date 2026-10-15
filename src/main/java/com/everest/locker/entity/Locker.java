package com.everest.locker.entity;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A physical locker. It owns its occupancy state: callers can only try to occupy it or
 * release it, never set its contents directly, so "one package at a time" is enforced here.
 *
 * <p>Thread-safe: many HTTP requests can touch the same locker at once (Level 4). Every state
 * change is a single atomic compare-and-set on {@link #occupant}, so "check it is empty" and
 * "put the parcel in" can never be split by another thread. No locks are held, so requests for
 * different lockers never wait for each other.
 */
public final class Locker {

    private final LockerId id;
    private final Size size;
    private final AtomicReference<StoredParcel> occupant = new AtomicReference<>();

    public Locker(LockerId id, Size size) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.size = Objects.requireNonNull(size, "size must not be null");
    }

    /**
     * Puts the parcel in this locker if it is empty. If several threads try at once, exactly
     * one succeeds.
     *
     * @return {@code true} if the parcel was stored, {@code false} if the locker was already occupied
     */
    public boolean tryOccupy(StoredParcel parcel) {
        Objects.requireNonNull(parcel, "parcel must not be null");
        return occupant.compareAndSet(null, parcel);
    }

    /**
     * Empties the locker.
     *
     * @return the parcel that was inside
     * @throws IllegalStateException if the locker is already empty
     */
    public StoredParcel release() {
        StoredParcel released = occupant.getAndSet(null);
        if (released == null) {
            throw new IllegalStateException("Locker " + id + " is already empty");
        }
        return released;
    }

    /**
     * Empties the locker only if the given code belongs to the parcel inside. If several threads
     * use the right code at once, exactly one gets the parcel.
     *
     * @return the released parcel, or empty if the locker is empty or the code does not match;
     *         in that case nothing changes
     */
    public Optional<StoredParcel> releaseIfCodeMatches(PickupCode code) {
        Objects.requireNonNull(code, "code must not be null");
        StoredParcel current = occupant.get();
        if (current == null || !current.pickupCode().equals(code)) {
            return Optional.empty();
        }
        // Succeeds only if nobody else changed the locker since we looked.
        return occupant.compareAndSet(current, null) ? Optional.of(current) : Optional.empty();
    }

    public LockerId id() {
        return id;
    }

    public Size size() {
        return size;
    }

    public boolean isAvailable() {
        return occupant.get() == null;
    }

    public LockerStatus status() {
        return isAvailable() ? LockerStatus.AVAILABLE : LockerStatus.OCCUPIED;
    }

    public Optional<StoredParcel> occupant() {
        return Optional.ofNullable(occupant.get());
    }
}
