package com.everest.locker.controller;

import static com.everest.locker.support.ConcurrentRunner.runTogether;
import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.support.ConcurrentRunner.Outcome;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Level 4 end to end: a real server on a random port, many real HTTP requests at once.
 * Each request is served on its own Tomcat thread, exactly like production traffic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class Level4ConcurrentHttpTest {

    private static final int LOCKERS = 10;
    private static final int AGENTS = 40;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void manyAgentsStoringAtOnceGetDistinctLockersAndTheRestAreTurnedAway() throws Exception {
        assertThat(post("/api/lockers", "{\"size\": \"SMALL\", \"count\": " + LOCKERS + "}").statusCode())
                .isEqualTo(201);

        List<Outcome<HttpResponse<String>>> outcomes = runTogether(AGENTS, () ->
                post("/api/packages", "{\"size\": \"SMALL\", \"customerContact\": \"+60123456789\"}"));

        List<HttpResponse<String>> responses = outcomes.stream().map(Outcome::result).toList();
        List<String> lockerIds = responses.stream()
                .filter(response -> response.statusCode() == 201)
                .map(response -> response.body().replaceAll(".*\"lockerId\":\"(L\\d+)\".*", "$1"))
                .toList();
        assertThat(lockerIds).hasSize(LOCKERS).doesNotHaveDuplicates();
        assertThat(responses)
                .filteredOn(response -> response.statusCode() != 201)
                .hasSize(AGENTS - LOCKERS)
                .allMatch(response -> response.statusCode() == 409
                        && response.body().contains("NO_SUITABLE_LOCKER"));

        String lockers = get("/api/lockers").body();
        assertThat(lockers).doesNotContain("AVAILABLE");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
