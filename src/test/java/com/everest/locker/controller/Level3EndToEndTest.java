package com.everest.locker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Demo profile with the default pricing (X = 1.00): fast-forward 12 days and collect. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Level3EndToEndTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void aPackageLeftForTwelveDaysCosts21Units() throws Exception {
        postJson("/api/lockers", """
                {"size": "SMALL"}""").andExpect(status().isCreated());
        String storedAt = JsonPath.read(postJson("/api/packages", """
                {"size": "SMALL", "customerContact": "+60123456789"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storedAt").exists())
                .andReturn().getResponse().getContentAsString(), "$.storedAt");
        String code = JsonPath.read(
                mvc.perform(get("/api/demo/inbox")).andReturn().getResponse().getContentAsString(),
                "$[0].pickupCode");

        postJson("/api/demo/clock/advance", """
                {"hours": 288}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.now").exists());

        postJson("/api/lockers/L1/retrieval", "{\"pickupCode\": \"" + code + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedAt").value(storedAt))
                .andExpect(jsonPath("$.chargeableDays").value(12))
                .andExpect(jsonPath("$.storageCharge").value(21.00));
    }

    @Test
    void theClockOnlyMovesForwardByAPositiveNumberOfHours() throws Exception {
        postJson("/api/demo/clock/advance", """
                {"hours": 0}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
