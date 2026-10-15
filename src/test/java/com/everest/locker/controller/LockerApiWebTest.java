package com.everest.locker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everest.locker.entity.LockerStatus;
import com.everest.locker.entity.Size;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.NoSuitableLockerException;
import com.everest.locker.exception.NotificationFailedException;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.LockerView;
import com.everest.locker.service.StoreResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Web-layer slice: checks HTTP mapping, validation and error responses.
 * The service is mocked here; its behaviour is covered by the service tests.
 */
@WebMvcTest(controllers = {LockerController.class, PackageController.class})
class LockerApiWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LockerService service;

    @Test
    void createLockersReturns201WithTheNewLockers() throws Exception {
        given(service.addLockers(Size.SMALL, 2)).willReturn(List.of(
                new LockerView("L1", Size.SMALL, LockerStatus.AVAILABLE),
                new LockerView("L2", Size.SMALL, LockerStatus.AVAILABLE)));

        mvc.perform(json(post("/api/lockers"), """
                        {"size": "SMALL", "count": 2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value("L1"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    void createLockersDefaultsCountToOneAndAcceptsLowerCaseSize() throws Exception {
        given(service.addLockers(Size.LARGE, 1))
                .willReturn(List.of(new LockerView("L1", Size.LARGE, LockerStatus.AVAILABLE)));

        mvc.perform(json(post("/api/lockers"), """
                        {"size": "large"}"""))
                .andExpect(status().isCreated());

        verify(service).addLockers(Size.LARGE, 1);
    }

    @Test
    void createLockersWithoutSizeReturns400() throws Exception {
        mvc.perform(json(post("/api/lockers"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("size: must not be null"));
    }

    @Test
    void unknownSizeReturns400() throws Exception {
        mvc.perform(json(post("/api/lockers"), """
                        {"size": "HUGE"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("SMALL, MEDIUM, LARGE")));
    }

    @Test
    void serviceValidationErrorsReturn400() throws Exception {
        given(service.addLockers(Size.SMALL, 500))
                .willThrow(new InvalidRequestException("Locker count must be between 1 and 100."));

        mvc.perform(json(post("/api/lockers"), """
                        {"size": "SMALL", "count": 500}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Locker count must be between 1 and 100."));
    }

    @Test
    void listLockersReturnsEveryLockerWithItsStatus() throws Exception {
        given(service.listLockers()).willReturn(List.of(
                new LockerView("L1", Size.SMALL, LockerStatus.OCCUPIED)));

        mvc.perform(get("/api/lockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("L1"))
                .andExpect(jsonPath("$[0].size").value("SMALL"))
                .andExpect(jsonPath("$[0].status").value("OCCUPIED"));
    }

    @Test
    void storePackageReturns201WithTheLockerButNotThePickupCode() throws Exception {
        given(service.storeParcel(Size.MEDIUM, "+60123456789"))
                .willReturn(new StoreResult("L2", Size.MEDIUM, Instant.parse("2026-01-01T09:00:00Z")));

        mvc.perform(json(post("/api/packages"), """
                        {"size": "MEDIUM", "customerContact": "+60123456789"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lockerId").value("L2"))
                .andExpect(jsonPath("$.lockerSize").value("MEDIUM"))
                .andExpect(jsonPath("$.storedAt").value("2026-01-01T09:00:00Z"))
                .andExpect(jsonPath("$.pickupCode").doesNotExist());
    }

    @Test
    void storePackageWithNoFreeLockerReturns409() throws Exception {
        given(service.storeParcel(any(), anyString())).willThrow(new NoSuitableLockerException(Size.LARGE));

        mvc.perform(json(post("/api/packages"), """
                        {"size": "LARGE", "customerContact": "+60123456789"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SUITABLE_LOCKER"))
                .andExpect(jsonPath("$.detail")
                        .value("No available locker can fit a LARGE package. The package cannot be stored."));
    }

    @Test
    void notificationFailureReturns503WithoutInternalDetails() throws Exception {
        given(service.storeParcel(any(), anyString()))
                .willThrow(new NotificationFailedException(new RuntimeException("smtp.internal:25 refused")));

        mvc.perform(json(post("/api/packages"), """
                        {"size": "SMALL", "customerContact": "+60123456789"}"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_FAILED"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(containsString("smtp"))));
    }

    @Test
    void unexpectedErrorsReturnAGeneric500() throws Exception {
        given(service.addLockers(any(), anyInt())).willThrow(new IllegalStateException("secret internals"));

        mvc.perform(json(post("/api/lockers"), """
                        {"size": "SMALL"}"""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(json(post("/api/packages"), "{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
