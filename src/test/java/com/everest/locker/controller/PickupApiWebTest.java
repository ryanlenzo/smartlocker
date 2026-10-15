package com.everest.locker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.Size;
import com.everest.locker.exception.InvalidPickupCodeException;
import com.everest.locker.exception.InvalidRequestException;
import com.everest.locker.exception.LockerEmptyException;
import com.everest.locker.exception.LockerNotFoundException;
import com.everest.locker.service.LockerService;
import com.everest.locker.service.RetrievalResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Web-layer slice for the customer pickup endpoint; the service is mocked. */
@WebMvcTest(controllers = PickupController.class)
class PickupApiWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LockerService service;

    @Test
    void aValidPickupReturns200WithTheOpenedLocker() throws Exception {
        given(service.retrieveParcel("L2", "123456")).willReturn(new RetrievalResult("L2", Size.MEDIUM));

        retrieve("L2", """
                {"pickupCode": "123456"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value("L2"))
                .andExpect(jsonPath("$.lockerSize").value("MEDIUM"));
    }

    @Test
    void aWrongCodeReturns403() throws Exception {
        given(service.retrieveParcel("L2", "000000")).willThrow(new InvalidPickupCodeException(new LockerId(2)));

        retrieve("L2", """
                {"pickupCode": "000000"}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_PICKUP_CODE"))
                .andExpect(jsonPath("$.detail").value("The pickup code is not valid for locker L2."));
    }

    @Test
    void anUnknownLockerReturns404() throws Exception {
        given(service.retrieveParcel("L99", "123456")).willThrow(new LockerNotFoundException("L99"));

        retrieve("L99", """
                {"pickupCode": "123456"}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCKER_NOT_FOUND"));
    }

    @Test
    void anEmptyLockerReturns409() throws Exception {
        given(service.retrieveParcel("L1", "123456")).willThrow(new LockerEmptyException(new LockerId(1)));

        retrieve("L1", """
                {"pickupCode": "123456"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOCKER_EMPTY"));
    }

    @Test
    void aMissingCodeReturns400() throws Exception {
        given(service.retrieveParcel("L1", null)).willThrow(new InvalidRequestException("Pickup code is required."));

        retrieve("L1", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Pickup code is required."));
    }

    @Test
    void aMissingBodyReturns400() throws Exception {
        mvc.perform(post("/api/lockers/L1/retrieval").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    private ResultActions retrieve(String lockerId, String body) throws Exception {
        return mvc.perform(post("/api/lockers/{id}/retrieval", lockerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
