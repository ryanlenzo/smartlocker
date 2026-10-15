package com.everest.locker.entity;

import java.util.Objects;

/**
 * A package handed over by a delivery agent.
 *
 * <p>Named {@code Parcel} in code because {@code Package} clashes with {@link java.lang.Package}.
 */
public record Parcel(Size size, String customerContact) {

    public Parcel {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(customerContact, "customerContact must not be null");
    }
}
