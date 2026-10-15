package com.everest.locker.entity;

import java.util.Objects;

/**
 * Size category shared by lockers and packages.
 *
 * <p>Constants are declared from smallest to largest; the declaration order is the size
 * order, so {@link #canHold(Size)} and "smallest available locker" both rely on it.
 */
public enum Size {
    SMALL,
    MEDIUM,
    LARGE;

    /**
     * Whether a locker of this size can store a package of the given size.
     * A locker holds packages of its own size or smaller.
     */
    public boolean canHold(Size packageSize) {
        Objects.requireNonNull(packageSize, "packageSize must not be null");
        return this.compareTo(packageSize) >= 0;
    }
}
