package com.everest.locker.controller;

import com.everest.locker.controller.dto.StorePackageRequest;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.StoreResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Delivery agent operations. */
@RestController
@RequestMapping("/api/packages")
public class PackageController {

    private final LockerService service;

    public PackageController(LockerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResult storePackage(@Valid @RequestBody StorePackageRequest request) {
        return service.storeParcel(request.size(), request.customerContact());
    }
}
