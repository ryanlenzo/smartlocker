package com.everest.locker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Full application wiring, no mocks: the Level 1 flow through real HTTP handling. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Level1EndToEndTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createStoreAndList() throws Exception {
        createLockers("LARGE", 1);
        createLockers("SMALL", 2);

        mvc.perform(post("/api/packages").contentType(MediaType.APPLICATION_JSON).content("""
                        {"size": "SMALL", "customerContact": "+60123456789"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lockerId").value("L2"));

        mvc.perform(get("/api/lockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].status").value("OCCUPIED"))
                .andExpect(jsonPath("$[2].status").value("AVAILABLE"));
    }

    @Test
    void storingWithoutAnyFittingLockerReturns409() throws Exception {
        createLockers("SMALL", 1);

        mvc.perform(post("/api/packages").contentType(MediaType.APPLICATION_JSON).content("""
                        {"size": "LARGE", "customerContact": "+60123456789"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SUITABLE_LOCKER"));
    }

    private void createLockers(String size, int count) throws Exception {
        mvc.perform(post("/api/lockers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"size\": \"%s\", \"count\": %d}".formatted(size, count)))
                .andExpect(status().isCreated());
    }
}
