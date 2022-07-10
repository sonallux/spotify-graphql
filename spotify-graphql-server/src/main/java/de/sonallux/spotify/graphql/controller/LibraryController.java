package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class LibraryController extends BaseController {
    @QueryMapping
    Map<String, Object> library() {
        return Map.of("albums", Map.of(), "episodes", Map.of(), "shows", Map.of(), "tracks", Map.of());
    }

    @SchemaMapping(typeName = "Library")
    Mono<Map<String, Object>> albums(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject("/me/albums", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "Library")
    Mono<Map<String, Object>> episodes(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject("/me/episodes", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "Library")
    Mono<Map<String, Object>> shows(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject("/me/shows", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "Library")
    Mono<Map<String, Object>> tracks(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject("/me/tracks", arguments, rawLoader);
    }
}
