package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class ShowController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> show(@Nullable @Argument String id, @Nullable @Argument String uri,
                                    DataLoader<String, Map<String, Object>> showLoader
    ) {
        return load(id, uri, "show", showLoader);
    }

    @QueryMapping
    Mono<List<Map<String, Object>>> shows(@Nullable @Argument List<String> ids, @Nullable @Argument List<String> uris,
                                           DataLoader<String, Map<String, Object>> showLoader
    ) {
        return loadMany(ids, uris, "show", showLoader);
    }

    @SchemaMapping(typeName = "Show")
    Mono<Map<String, Object>> episodes(Map<String, Object> show, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject(show, arguments, "episodes", rawLoader);
    }
}
