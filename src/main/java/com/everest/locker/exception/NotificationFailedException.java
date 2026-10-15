package com.everest.locker.exception;

/**
 * The customer could not be sent their pickup code, so the package was not stored:
 * a package nobody can collect would block the locker indefinitely.
 */
public final class NotificationFailedException extends LockerException {

    public NotificationFailedException(Throwable cause) {
        super("The customer could not be notified, so the package was not stored. Please try again.", cause);
    }
}
