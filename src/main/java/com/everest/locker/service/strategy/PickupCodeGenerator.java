package com.everest.locker.service.strategy;

import com.everest.locker.entity.PickupCode;

/** Produces candidate pickup codes. Uniqueness is checked by the caller. */
@FunctionalInterface
public interface PickupCodeGenerator {

    PickupCode next();
}
