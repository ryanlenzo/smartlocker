package com.everest.locker.service.strategy;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.Size;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Prefers the smallest available locker that fits, keeping bigger lockers free for bigger
 * packages. Among lockers of the same size, the lowest number wins so results are predictable.
 */
public class SmallestFitSelectionPolicy implements LockerSelectionPolicy {

    private static final Comparator<Locker> SMALLEST_FIRST =
            Comparator.comparing(Locker::size).thenComparing(Locker::id);

    @Override
    public List<Locker> candidates(List<Locker> lockers, Size packageSize) {
        Objects.requireNonNull(packageSize, "packageSize must not be null");
        return lockers.stream()
                .filter(Locker::isAvailable)
                .filter(locker -> locker.size().canHold(packageSize))
                .sorted(SMALLEST_FIRST)
                .toList();
    }
}
