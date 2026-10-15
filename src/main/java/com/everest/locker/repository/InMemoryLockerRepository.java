package com.everest.locker.repository;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerId;
import com.everest.locker.repository.LockerRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps lockers in memory; data is lost on restart (see README next steps).
 * Thread-safe collections because the Spring bean is shared by all HTTP request threads.
 */
public class InMemoryLockerRepository implements LockerRepository {

    private final Map<LockerId, Locker> lockers = new ConcurrentHashMap<>();
    private final AtomicInteger lastNumber = new AtomicInteger();

    @Override
    public LockerId nextId() {
        return new LockerId(lastNumber.incrementAndGet());
    }

    @Override
    public void save(Locker locker) {
        lockers.put(locker.id(), locker);
    }

    @Override
    public List<Locker> findAll() {
        return lockers.values().stream()
                .sorted(Comparator.comparing(Locker::id))
                .toList();
    }
}
