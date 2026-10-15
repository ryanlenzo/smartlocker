package com.everest.locker.exception;

import com.everest.locker.entity.LockerId;

/** There is no package in the locker, e.g. it was already collected. */
public final class LockerEmptyException extends LockerException {

    public LockerEmptyException(LockerId lockerId) {
        super("Locker " + lockerId + " is empty. There is no package to collect.");
    }
}
