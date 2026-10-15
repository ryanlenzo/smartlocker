package com.everest.locker.hardware;

import com.everest.locker.entity.LockerId;

/**
 * The physical door of a locker. Real hardware is out of scope, so only this interface and a
 * stand-in implementation exist.
 */
@FunctionalInterface
public interface LockerDoor {

    /**
     * Unlocks and opens the door.
     *
     * @throws RuntimeException if the door could not be opened
     */
    void open(LockerId lockerId);
}
