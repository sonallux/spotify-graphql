package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class UserController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> me(DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/me", rawLoader);
    }

    @SchemaMapping(typeName = "PrivateUser", field = "playlists")
    Mono<Map<String, Object>> mePlaylists(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/me/playlists", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "PrivateUser", field = "top_artists")
    Mono<Map<String, Object>> meTopArtists(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/me/top/artists", arguments, rawLoader);
    }

    @SchemaMapping(typeName = "PrivateUser", field = "top_tracks")
    Mono<Map<String, Object>> meTopTracks(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/me/top/tracks", arguments, rawLoader);
    }

    @QueryMapping
    Mono<Map<String, Object>> user(@Nullable @Argument String id, @Nullable @Argument String uri,
                                       DataLoader<String, Map<String, Object>> userLoader
    ) {
        return load(id, uri, "user", userLoader);
    }

    @SchemaMapping(typeName = "PublicUser", field = "playlists")
    Mono<Map<String, Object>> userPlaylists(Map<String, Object> user, @Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadPagingObject(user, arguments, "playlists", rawLoader);
    }
}
