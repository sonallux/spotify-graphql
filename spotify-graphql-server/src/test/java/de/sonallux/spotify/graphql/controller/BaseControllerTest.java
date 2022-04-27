package de.sonallux.spotify.graphql.controller;

import de.sonallux.spotify.graphql.MockWebServerJUnitExtension;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@SpringBootTest(properties = "spotify.base-url=http://localhost:" + MockWebServerJUnitExtension.PORT)
@ExtendWith(MockWebServerJUnitExtension.class)
public abstract class BaseControllerTest {
    @Autowired
    WebGraphQlHandler webGraphQlHandler;

    GraphQlTester graphQlTester;

    MockWebServer mockWebServer;

    @BeforeEach
    void setUp() {
        graphQlTester = WebGraphQlTester
            .builder(webGraphQlHandler)
            .header(HttpHeaders.AUTHORIZATION, "spotify_test_auth_token")
            .build();
    }

    MockResponse createJsonResponse(String jsonBody) {
        return new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(jsonBody);
    }
}
