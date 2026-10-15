package com.everest.locker.exception;

/**
 * Base class for expected, user-facing failures. Messages are written for the end user.
 *
 * <p>Sealed so that every subtype is known at compile time: the controller layer maps them with an
 * exhaustive {@code switch}, and adding a new failure type will not compile until it is mapped.
 */
public abstract sealed class LockerException extends RuntimeException
        permits InvalidRequestException, NoSuitableLockerException, NotificationFailedException {

    protected LockerException(String message) {
        super(message);
    }

    protected LockerException(String message, Throwable cause) {
        super(message, cause);
    }
}
