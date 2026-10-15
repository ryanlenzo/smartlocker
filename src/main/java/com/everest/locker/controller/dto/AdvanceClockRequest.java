package com.everest.locker.controller.dto;

/** Body of {@code POST /api/demo/clock/advance} (demo profile only). */
public record AdvanceClockRequest(Integer hours) {
}
