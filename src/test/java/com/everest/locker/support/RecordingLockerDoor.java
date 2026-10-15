package com.everest.locker.support;

import com.everest.locker.entity.LockerId;
import com.everest.locker.hardware.LockerDoor;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test fake: remembers which locker doors were opened. */
public class RecordingLockerDoor implements LockerDoor {

    private final List<LockerId> opened = new CopyOnWriteArrayList<>();

    @Override
    public void open(LockerId lockerId) {
        opened.add(lockerId);
    }

    public List<LockerId> opened() {
        return List.copyOf(opened);
    }
}
