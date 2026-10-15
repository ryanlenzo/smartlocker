package com.everest.locker.entity;

/** The secret a customer uses to open a locker. */
public record PickupCode(String value) {

    public PickupCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pickup code must not be blank");
        }
    }

    /** Never print the secret by accident, e.g. in logs or exception messages. */
    @Override
    public String toString() {
        return "PickupCode[******]";
    }
}
