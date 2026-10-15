package com.everest.locker.exception;

/** The customer asked for a locker that does not exist. */
public final class LockerNotFoundException extends LockerException {

    public LockerNotFoundException(String lockerId) {
        super("Locker " + lockerId + " does not exist.");
    }
}
