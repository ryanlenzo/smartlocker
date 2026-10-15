package com.everest.locker.entity;

import java.util.Objects;

/** A parcel sitting in a locker, together with the code that releases it. */
public record StoredParcel(Parcel parcel, PickupCode pickupCode) {

    public StoredParcel {
        Objects.requireNonNull(parcel, "parcel must not be null");
        Objects.requireNonNull(pickupCode, "pickupCode must not be null");
    }
}
