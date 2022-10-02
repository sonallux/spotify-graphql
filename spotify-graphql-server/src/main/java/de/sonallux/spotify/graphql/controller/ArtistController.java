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
public class ArtistController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> artist(@Nullable @Argument String id, @Nullable @Argument String uri,
                                    DataLoader<String, Map<String, Object>> artistLoader
    ) {
        return load(id, uri, "artist", artistLoader);
    }

    @QueryMapping
    Mono<List<Map<String, Object>>> artists(@Nullable @Argument List<String> ids, @Nullable @Argument List<String> uris,
                                            DataLoader<String, Map<String, Object>> artistLoader
    ) {
        return loadMany(ids, uris, "artist", artistLoader);
    }

    @SchemaMapping(typeName = "Artist")
    Mono<Map<String, Object>> albums(Map<String, Object> artist, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject(artist, arguments, "albums", rawLoader);
    }

    @SchemaMapping(typeName = "Artist", field = "related_artists")
    Mono<List<Map<String, Object>>> relatedArtists(Map<String, Object> artist, DataLoader<String, Map<String, Object>> rawLoader) {
        var id = (String) artist.get("id");
        return loadRawObject(String.format("/artists/%s/related-artists", id), rawLoader)
            .map(result -> (List<Map<String, Object>>)result.get("artists"));
    }

    @SchemaMapping(typeName = "Artist", field = "top_tracks")
    Mono<List<Map<String, Object>>> topTracks(Map<String, Object> artist, DataLoader<String, Map<String, Object>> rawLoader) {
        var id = (String) artist.get("id");
        return loadRawObject(String.format("/artists/%s/top-tracks?market=from_token", id), rawLoader)
            .map(result -> (List<Map<String, Object>>)result.get("tracks"));
    }
}
