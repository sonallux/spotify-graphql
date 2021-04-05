package de.sonallux.spotify.graphql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@AllArgsConstructor
public class HttpClient {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public <T> T request(Request request, TypeReference<T> type) throws IOException {
        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return objectMapper.readValue(response.body().charStream(), type);
            }
            throw toHttpClientException(response);
        }
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
