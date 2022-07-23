package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BrowseController extends BaseController {
    @QueryMapping
    Map<String, Object> browse() {
        return Map.of("categories", Map.of(), "category", Map.of());
    }

    @SchemaMapping(typeName = "Browse")
    Mono<Object> categories(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/browse/categories", arguments, rawLoader)
            .map(response -> response.get("categories"));
    }

    @SchemaMapping(typeName = "Browse")
    Mono<Map<String, Object>> category(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        var mutableArguments = new HashMap<>(arguments);
        var id = mutableArguments.remove("category_id");
        return loadRawObject(String.format("/browse/categories/%s", id), mutableArguments, rawLoader);
    }

    @SchemaMapping(typeName = "Category")
    Mono<Object> playlists(Map<String, Object> category, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        var categoryId = category.get("id");
        return loadRawObject(String.format("/browse/categories/%s/playlists", categoryId), arguments, rawLoader)
            .map(response -> response.get("playlists"));
    }

    @SchemaMapping(typeName = "Browse", field = "featured_playlists")
    Mono<Map<String, Object>> featuredPlaylists(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/browse/featured-playlists", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "Browse", field = "new_releases")
    Mono<Object> newReleases(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/browse/new-releases", arguments, rawLoader)
            .map(response -> response.get("albums"));
    }

    @SchemaMapping(typeName = "Browse")
    Mono<Map<String, Object>> recommendations(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        if (!arguments.containsKey("seed_artists") && !arguments.containsKey("seed_genres") && !arguments.containsKey("seed_tracks")) {
            return Mono.error(new IllegalArgumentException("Require between one and 5 seed values in any combination of `seed_artists`, `seed_tracks` and `seed_genres`"));
        }
        return loadRawObject("/recommendations", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "Browse", field = "recommendations_genre_seeds")
    Mono<Object> recommendationsGenreSeeds(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/recommendations/available-genre-seeds", arguments, rawLoader)
            .map(response -> response.get("genres"));
    }
}
