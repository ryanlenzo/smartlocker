package com.everest.locker.hardware;

import com.everest.locker.entity.LockerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stand-in for the locker hardware: logs that the door opened. */
public class LoggingLockerDoor implements LockerDoor {

    private static final Logger log = LoggerFactory.getLogger(LoggingLockerDoor.class);

    @Override
    public void open(LockerId lockerId) {
        log.info("[simulated hardware] Locker {} door opened", lockerId);
    }
}
