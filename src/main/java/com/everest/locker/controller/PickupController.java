package com.everest.locker.controller;

import com.everest.locker.controller.dto.RetrievePackageRequest;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.RetrievalResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Customer operations. */
@RestController
public class PickupController {

    private final LockerService service;

    public PickupController(LockerService service) {
        this.service = service;
    }

    /** POST (not GET) because it changes state, and the code belongs in the body, not the URL or logs. */
    @PostMapping("/api/lockers/{lockerId}/retrieval")
    public RetrievalResult retrievePackage(@PathVariable String lockerId,
                                           @RequestBody RetrievePackageRequest request) {
        return service.retrieveParcel(lockerId, request.pickupCode());
    }
}
