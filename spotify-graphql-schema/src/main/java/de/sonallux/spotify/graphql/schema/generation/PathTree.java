package de.sonallux.spotify.graphql.schema.generation;

import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class PathTree {
    private Entry root;

    public Entry generatePathTree(SpotifyWebApi spotifyWebApi) {
        root = new Entry("");

        spotifyWebApi.getCategoryList().stream()
            .flatMap(c -> c.getEndpointList().stream())
            .filter(e -> e.getHttpMethod().equals("GET"))
            .forEach(this::addEndpoint);

        return root;
    }

    private void addEndpoint(SpotifyWebApiEndpoint endpoint) {
        var pathSegments = endpoint.getPath().split("/");
        if (!"".equals(pathSegments[0])) {
            throw new IllegalStateException("Endpoint has wrong path root: " + pathSegments[0]);
        }

        var currentNode = root;
        for (int i = 1; i < pathSegments.length; i++) {
            currentNode = currentNode.getOrComputeChild(pathSegments[i]);
        }
        currentNode.setEndpoint(endpoint);
    }

    @Getter
    @AllArgsConstructor
    public static class Entry {
        private final String path;
        private final Map<String, Entry> children;
        @Setter
        private SpotifyWebApiEndpoint endpoint;

        public Entry(String path) {
            this(path, new HashMap<>(), null);
        }

        public Entry(String path, SpotifyWebApiEndpoint endpoint) {
            this(path, new HashMap<>(), endpoint);
        }

        public Entry getOrComputeChild(String path) {
            return children.computeIfAbsent(path, Entry::new);
        }
    }
}
