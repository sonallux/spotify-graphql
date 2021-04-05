package de.sonallux.spotify.graphql;

import lombok.Getter;

import java.io.IOException;

public class HttpClientException extends IOException {

    @Getter
    private final int code;

    @Getter
    private final String body;

    public HttpClientException(String message) {
        super(message);
        this.code = -1;
        this.body = null;
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
        this.body = null;
    }

    public HttpClientException(int code, String message, String body) {
        super(code + ": " + message);
        this.code = code;
        this.body = body;
    }

    public HttpClientException(int code, String message) {
        this(code, message, null);
    }
}
