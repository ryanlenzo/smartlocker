package com.everest.locker.entity;

/**
 * Identifier of a locker, shown as {@code L1}, {@code L2}, ...
 *
 * <p>Kept numeric so lockers sort as L2 before L10, which plain string ordering gets wrong.
 */
public record LockerId(int number) implements Comparable<LockerId> {

    public LockerId {
        if (number < 1) {
            throw new IllegalArgumentException("Locker number must be positive, was " + number);
        }
    }

    @Override
    public int compareTo(LockerId other) {
        return Integer.compare(number, other.number);
    }

    @Override
    public String toString() {
        return "L" + number;
    }
}
