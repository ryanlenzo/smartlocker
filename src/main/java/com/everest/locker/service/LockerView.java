package com.everest.locker.service;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerStatus;
import com.everest.locker.entity.Size;

/** Read-only snapshot of a locker, safe to hand to any interface (REST, UI, CLI). */
public record LockerView(String id, Size size, LockerStatus status) {

    static LockerView of(Locker locker) {
        return new LockerView(locker.id().toString(), locker.size(), locker.status());
    }
}
