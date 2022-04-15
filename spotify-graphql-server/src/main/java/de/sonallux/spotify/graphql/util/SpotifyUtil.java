package de.sonallux.spotify.graphql.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpotifyUtil {
    public static Optional<String> getTypeFromUri(String uri) {
        var parts = uri.split(":");
        if (parts.length != 3 && !"spotify".equals(parts[0])) {
            return Optional.empty();
        }
        return Optional.of(parts[1]);
    }

    public static Optional<String> getIdFromUri(String uri) {
        var parts = uri.split(":");
        if (parts.length != 3 && !"spotify".equals(parts[0])) {
            return Optional.empty();
        }
        return Optional.of(parts[2]);
    }
}
