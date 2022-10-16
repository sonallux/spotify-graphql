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
public class AudiobookController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> audiobook(@Nullable @Argument String id, @Nullable @Argument String uri,
                                        DataLoader<String, Map<String, Object>> audiobookLoader
    ) {
        return load(id, uri, "audiobook", audiobookLoader);
    }

    @QueryMapping
    Mono<List<Map<String, Object>>> audiobooks(@Nullable @Argument List<String> ids, @Nullable @Argument List<String> uris,
                                               DataLoader<String, Map<String, Object>> audiobookLoader
    ) {
        return loadMany(ids, uris, "audiobook", audiobookLoader);
    }

    @SchemaMapping(typeName = "Audiobook")
    Mono<Map<String, Object>> chapters(Map<String, Object> audiobook, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject(audiobook, arguments, "chapters", rawLoader);
    }
}
