package com.everest.locker.exception;

import com.everest.locker.entity.LockerId;

/** The pickup code does not belong to the package in this locker. */
public final class InvalidPickupCodeException extends LockerException {

    public InvalidPickupCodeException(LockerId lockerId) {
        super("The pickup code is not valid for locker " + lockerId + ".");
    }
}
