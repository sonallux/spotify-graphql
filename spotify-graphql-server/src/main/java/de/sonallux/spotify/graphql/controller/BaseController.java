package de.sonallux.spotify.graphql.controller;

import de.sonallux.spotify.graphql.util.SpotifyUtil;
import org.dataloader.DataLoader;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class BaseController {

    Mono<Map<String, Object>> load(@Nullable String id, @Nullable String uri, String type,
                                             DataLoader<String, Map<String, Object>> dataloader) {
        return Mono.fromSupplier(() -> extractSpotifyId(id, uri, type))
            .flatMap(actualId -> Mono.fromFuture(dataloader.load(actualId)));
    }

    Mono<List<Map<String, Object>>> loadMany(@Nullable List<String> ids, @Nullable List<String> uris, String type,
                                             DataLoader<String, Map<String, Object>> dataloader) {
        return Mono.fromSupplier(() -> extractSpotifyIds(ids, uris, type))
            .flatMap(actualIds -> Mono.fromFuture(dataloader.loadMany(actualIds)));
    }

    String extractSpotifyId(@Nullable String id, @Nullable String uri, String type) {
        if (id != null && uri == null) {
            // id present
            return id;
        } else if (id == null && uri != null) {
            // uri present
            if (SpotifyUtil.getTypeFromUri(uri).filter(actualType -> Objects.equals(actualType, type)).isEmpty()) {
                throw new IllegalArgumentException("Expected a 'uri' of type '" + type + "' but got 'uri' " + uri);
            }
            return SpotifyUtil.getIdFromUri(uri)
                .orElseThrow(() -> new IllegalArgumentException("Missing 'id' in provided 'uri' " + uri));
        } else {
            throw new IllegalArgumentException("Either an 'id' or 'uri' must be specified");
        }
    }

    List<String> extractSpotifyIds(@Nullable List<String> ids, @Nullable List<String> uris, String type) {
        if (ids != null && uris == null) {
            // id present
            return ids;
        } else if (ids == null && uris != null) {
            // uri present
            return uris.stream().map(uri -> {
                if (SpotifyUtil.getTypeFromUri(uri).filter(actualType -> Objects.equals(actualType, type)).isEmpty()) {
                    throw new IllegalArgumentException("Expected a 'uri' of type '" + type + "' but got 'uri' " + uri);
                }
                return SpotifyUtil.getIdFromUri(uri)
                    .orElseThrow(() -> new IllegalArgumentException("Missing 'id' in provided 'uri' " + uri));
            }).toList();
        } else {
            throw new IllegalArgumentException("Either an 'ids' or 'uris' must be specified");
        }
    }
}
