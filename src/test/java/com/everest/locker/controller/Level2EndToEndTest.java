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

/**
 * Full application in the demo profile: store a package, read the code from the customer's
 * demo inbox, then collect the package with it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Level2EndToEndTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void storeThenCollectWithTheCodeFromTheInbox() throws Exception {
        postJson("/api/lockers", """
                {"size": "SMALL", "count": 1}""").andExpect(status().isCreated());
        postJson("/api/packages", """
                {"size": "SMALL", "customerContact": "+60123456789"}""").andExpect(status().isCreated());

        String inbox = mvc.perform(get("/api/demo/inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerContact").value("+60123456789"))
                .andExpect(jsonPath("$[0].lockerId").value("L1"))
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(inbox, "$[0].pickupCode");

        postJson("/api/lockers/L1/retrieval", "{\"pickupCode\": \"" + code + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value("L1"));

        mvc.perform(get("/api/lockers"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        postJson("/api/lockers/L1/retrieval", "{\"pickupCode\": \"" + code + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOCKER_EMPTY"));
    }

    @Test
    void aWrongCodeLeavesThePackageInTheLocker() throws Exception {
        postJson("/api/lockers", """
                {"size": "SMALL"}""").andExpect(status().isCreated());
        postJson("/api/packages", """
                {"size": "SMALL", "customerContact": "+60123456789"}""").andExpect(status().isCreated());

        String inbox = mvc.perform(get("/api/demo/inbox")).andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(inbox, "$[0].pickupCode");
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        postJson("/api/lockers/L1/retrieval", "{\"pickupCode\": \"" + wrongCode + "\"}")
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/lockers"))
                .andExpect(jsonPath("$[0].status").value("OCCUPIED"));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
