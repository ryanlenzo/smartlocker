package com.everest.locker.controller;

import com.everest.locker.controller.dto.CreateLockersRequest;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.LockerView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lockers")
public class LockerController {

    private final LockerService service;

    public LockerController(LockerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<LockerView> createLockers(@Valid @RequestBody CreateLockersRequest request) {
        return service.addLockers(request.size(), request.countOrDefault());
    }

    @GetMapping
    public List<LockerView> listLockers() {
        return service.listLockers();
    }
}
