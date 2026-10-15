package com.everest.locker.controller.dto;

/** Body of {@code POST /api/lockers/{id}/retrieval}. The code is checked by the service. */
public record RetrievePackageRequest(String pickupCode) {
}
