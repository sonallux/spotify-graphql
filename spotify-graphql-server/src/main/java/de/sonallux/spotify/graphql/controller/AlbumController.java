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
public class AlbumController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> album(@Nullable @Argument String id, @Nullable @Argument String uri,
                                    DataLoader<String, Map<String, Object>> albumLoader
    ) {
        return load(id, uri, "album", albumLoader);
    }

    @QueryMapping
    Mono<List<Map<String, Object>>> albums(@Nullable @Argument List<String> ids, @Nullable @Argument List<String> uris,
                                           DataLoader<String, Map<String, Object>> albumLoader
    ) {
        return loadMany(ids, uris, "album", albumLoader);
    }

    @SchemaMapping(typeName = "Album")
    Mono<Map<String, Object>> tracks(Map<String, Object> album, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject(album, arguments, "tracks", rawLoader);
    }
}
