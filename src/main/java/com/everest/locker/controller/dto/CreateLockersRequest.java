package com.everest.locker.controller.dto;

import com.everest.locker.entity.Size;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code POST /api/lockers}. {@code count} is optional and defaults to 1.
 *
 * <p>The controller only checks the request's shape. Business limits (such as the maximum
 * count) are enforced once, in the service, so every interface gets the same rules.
 */
public record CreateLockersRequest(@NotNull Size size, Integer count) {

    public int countOrDefault() {
        return count == null ? 1 : count;
    }
}
