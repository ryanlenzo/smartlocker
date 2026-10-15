package com.everest.locker.entity;

import java.time.Instant;
import java.util.Objects;

/** A parcel sitting in a locker, with the code that releases it and when it was put in. */
public record StoredParcel(Parcel parcel, PickupCode pickupCode, Instant storedAt) {

    public StoredParcel {
        Objects.requireNonNull(parcel, "parcel must not be null");
        Objects.requireNonNull(pickupCode, "pickupCode must not be null");
        Objects.requireNonNull(storedAt, "storedAt must not be null");
    }
}
