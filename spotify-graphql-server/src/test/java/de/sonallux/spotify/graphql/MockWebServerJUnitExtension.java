package de.sonallux.spotify.graphql;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.extension.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MockWebServerJUnitExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback, TestInstancePostProcessor {
    public static final int PORT = 8765;
    private MockWebServer mockWebServer;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start(PORT);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        mockWebServer.shutdown();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var missedRequests = getRecordedRequests();
        if (!missedRequests.isEmpty()) {
            throw new IllegalStateException("Missed recorded requests: " + missedRequests.stream()
                .map(request -> request.getMethod() + " " + request.getPath())
                .collect(Collectors.joining(", ")));
        }
    }

    private List<RecordedRequest> getRecordedRequests() throws Exception {
        var list = new ArrayList<RecordedRequest>();
        RecordedRequest recordedRequest;
        while ((recordedRequest = mockWebServer.takeRequest(1, TimeUnit.MILLISECONDS)) != null) {
            list.add(recordedRequest);
        }
        return list;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        ReflectionTestUtils.setField(testInstance, "mockWebServer", mockWebServer);
    }
}
