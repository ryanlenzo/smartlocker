package com.everest.locker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The demo inbox exposes pickup codes, so it must not exist outside the demo profile. */
@SpringBootTest
@AutoConfigureMockMvc
class DemoEndpointsDisabledByDefaultTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void theInboxIsNotAvailableWithoutTheDemoProfile() throws Exception {
        mvc.perform(get("/api/demo/inbox")).andExpect(status().isNotFound());
    }
}
