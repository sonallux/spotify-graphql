package de.sonallux.spotify.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;

@Slf4j
@AllArgsConstructor
public class HttpClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpClient() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public Response request(Request request) throws IOException {
        log.info("Executing request: {}", request.url());
        var response = httpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response;
        }
        throw toHttpClientException(response);
    }

    public <T> T request(Request request, TypeReference<T> type) throws IOException {
        try (var response = request(request)) {
            return objectMapper.readValue(response.body().charStream(), type);
        }
    }

    public RequestBody createJsonBody(Object body) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        return RequestBody.create(bytes, JSON_MEDIA_TYPE);
    }

    private HttpClientException toHttpClientException(Response response) {
        var bodyAsString = getBody(response);
        if (bodyAsString != null) {
            try {
                var errorBody = objectMapper.readTree(bodyAsString);
                var errorMsg = errorBody.get("error").get("message").asText();
                return new HttpClientException(response.code(), errorMsg, bodyAsString);
            } catch (IOException ignore) {}
        }
        return new HttpClientException(response.code(), "Failed to perform request", bodyAsString);
    }

    private String getBody(Response response) {
        if (response.body() == null) {
            return null;
        }
        try {
            return response.body().string();
        }
        catch (IOException ignore) {}
        return null;
    }
}
