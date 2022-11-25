package de.sonallux.spotify.graphql.controller;

import de.sonallux.spotify.graphql.util.SpotifyUtil;
import org.dataloader.DataLoader;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    Mono<Map<String, Object>> loadRawObject(String url, Map<String, Object> arguments,
                                            DataLoader<String, Map<String, Object>> rawLoader) {
        var urlWithQuery = String.format("%s%s", url, queryStringFromArguments(arguments));
        return loadRawObject(urlWithQuery, rawLoader);
    }

    Mono<Map<String, Object>> loadRawObject(String url, DataLoader<String, Map<String, Object>> rawLoader) {
        return Mono.fromFuture(rawLoader.load(url));
    }

    Mono<Map<String, Object>> loadPagingObject(Map<String, Object> parentObject, Map<String, Object> arguments,
                                               String property, DataLoader<String, Map<String, Object>> rawLoader) {
        var id = (String) parentObject.get("id");
        var parentType = (String) parentObject.get("type");

        var limitArgument = (Integer) arguments.get("limit");
        var offsetArgument = (Integer) arguments.get("offset");

        var existingPagingObject = (Map<String, Object>)parentObject.get(property);
        if (existingPagingObject != null && arguments.size() == 2 && arePagingArgumentsMatch(existingPagingObject, limitArgument, offsetArgument)) {
            return Mono.just(existingPagingObject);
        }

        return Mono.fromFuture(rawLoader.load(String.format("/%ss/%s/%s%s", parentType, id, property, queryStringFromArguments(arguments))));
    }

    private boolean arePagingArgumentsMatch(Map<String, Object> pagingObject, int limitArgument, int offsetArgument) {
        var limit = (Integer) pagingObject.get("limit");
        var offset = (Integer) pagingObject.get("offset");

        return Objects.equals(limit, limitArgument) && Objects.equals(offset, offsetArgument);
    }

    String queryStringFromArguments(Map<String, Object> arguments) {
        if (arguments.isEmpty()) {
            return "";
        }

        return "?" + arguments.entrySet().stream()
            .map(e -> e.getKey() + "=" + convertToQueryParam(e.getValue()))
            .collect(Collectors.joining("&"));
    }

    private String convertToQueryParam(Object object) {
        if (object instanceof List<?> list) {
            return list.stream().map(this::convertToQueryParam).collect(Collectors.joining(","));
        }
        return UriUtils.encodeQueryParam(object.toString(), StandardCharsets.UTF_8);
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
