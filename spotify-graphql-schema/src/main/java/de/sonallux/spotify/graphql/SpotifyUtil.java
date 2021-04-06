package de.sonallux.spotify.graphql;

public class SpotifyUtil {
    public static String getTypeFromUri(String uri) {
        var parts = uri.split(":");
        if (parts.length != 3 && !"spotify".equals(parts[0])) {
            return null;
        }
        return parts[1];
    }

    public static String getIdFromUri(String uri) {
        var parts = uri.split(":");
        if (parts.length != 3 && !"spotify".equals(parts[0])) {
            return null;
        }
        return parts[2];
    }
}
