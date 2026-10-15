package com.everest.locker.exception;

/** The request breaks a business rule on its input, e.g. a blank customer contact. */
public final class InvalidRequestException extends LockerException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
