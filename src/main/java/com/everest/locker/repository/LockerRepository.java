package com.everest.locker.repository;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerId;
import java.util.List;

/** Where lockers are kept. In memory today; a database implementation would implement this same interface. */
public interface LockerRepository {

    /** Reserves the next unused locker id. Ids are never reused. */
    LockerId nextId();

    void save(Locker locker);

    /** All lockers, ordered by id. */
    List<Locker> findAll();
}
