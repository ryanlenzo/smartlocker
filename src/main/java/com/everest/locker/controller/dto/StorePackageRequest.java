package com.everest.locker.controller.dto;

import com.everest.locker.entity.Size;
import jakarta.validation.constraints.NotNull;

/** Body of {@code POST /api/packages}. Contact rules are checked by the service. */
public record StorePackageRequest(@NotNull Size size, String customerContact) {
}
