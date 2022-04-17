package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class TrackController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> track(@Nullable @Argument String id, @Nullable @Argument String uri,
                                    DataLoader<String, Map<String, Object>> trackLoader
    ) {
        return load(id, uri, "track", trackLoader);
    }

    @QueryMapping
    Mono<List<Map<String, Object>>> tracks(@Nullable @Argument List<String> ids, @Nullable @Argument List<String> uris,
                                           DataLoader<String, Map<String, Object>> trackLoader
    ) {
        return loadMany(ids, uris, "track", trackLoader);
    }
}
