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
        return loadPagingObject("/browse/categories", arguments, rawLoader)
            .map(response -> response.get("categories"));
    }

    @SchemaMapping(typeName = "Browse")
    Mono<Map<String, Object>> category(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        var mutableArguments = new HashMap<>(arguments);
        var id = mutableArguments.remove("category_id");
        var queryString = queryStringFromArguments(mutableArguments);

        return Mono.fromFuture(rawLoader.load(String.format("/browse/categories/%s%s", id, queryString)));
    }

    @SchemaMapping(typeName = "Category")
    Mono<Object> playlists(Map<String, Object> category, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        var categoryId = category.get("id");
        return loadPagingObject(String.format("/browse/categories/%s/playlists", categoryId), arguments, rawLoader)
            .map(response -> response.get("playlists"));
    }
}
