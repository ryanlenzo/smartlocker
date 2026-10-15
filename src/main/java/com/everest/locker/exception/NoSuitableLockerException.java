package com.everest.locker.exception;

import com.everest.locker.entity.Size;

/** No available locker is big enough for the package. */
public final class NoSuitableLockerException extends LockerException {

    public NoSuitableLockerException(Size packageSize) {
        super("No available locker can fit a " + packageSize + " package. The package cannot be stored.");
    }
}
