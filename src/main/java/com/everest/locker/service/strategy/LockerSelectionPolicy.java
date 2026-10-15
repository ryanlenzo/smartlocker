package com.everest.locker.service.strategy;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.Size;
import java.util.List;

/** Decides which lockers may take a package, and in what order of preference. */
@FunctionalInterface
public interface LockerSelectionPolicy {

    /**
     * Returns the available lockers that can hold a package of the given size, most preferred
     * first. A list rather than a single locker lets the caller fall back to the next choice if
     * the preferred one is taken in the meantime.
     */
    List<Locker> candidates(List<Locker> lockers, Size packageSize);
}
