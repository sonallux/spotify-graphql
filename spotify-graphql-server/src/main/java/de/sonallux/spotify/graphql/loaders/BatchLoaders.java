package de.sonallux.spotify.graphql.loaders;

import com.google.common.collect.Lists;
import de.sonallux.spotify.graphql.exception.MissingAuthorizationException;
import de.sonallux.spotify.graphql.exception.ObjectNotFoundException;
import de.sonallux.spotify.graphql.security.AuthenticationGraphQlInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.Try;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class BatchLoaders {
    private static final ParameterizedTypeReference<Map<String, List<Map<String, Object>>>> OBJECTS_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> OBJECT_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public BatchLoaders(WebClient webClient, BatchLoaderRegistry batchLoaderRegistry) {
        this.webClient = webClient;

        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("albumLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("albums", 20, ids, env));
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("artistLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("artists", 50, ids, env));
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("audiobookLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("audiobooks", 50, ids, env));
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("chapterLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("chapters", 50, ids, env));
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("episodeLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("episodes", 50, ids, env));
        batchLoaderRegistry.<String, Map<String, Object>>forName("playlistLoader").registerBatchLoader(this::queryPlaylists);
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("showLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("shows", 50, ids, env));
        batchLoaderRegistry.<String, Try<Map<String, Object>>>forName("trackLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("tracks", 50, ids, env));
        batchLoaderRegistry.<String, Map<String, Object>>forName("userLoader").registerBatchLoader(this::queryUsers);

        batchLoaderRegistry.<String, Map<String, Object>>forName("rawLoader").registerBatchLoader((urls, env) ->
            queryUrls(urls));
    }

    private Flux<Try<Map<String, Object>>> queryBaseObjects(String type, int maxIdsPerQuery, List<String> ids, BatchLoaderEnvironment env) {
        return Flux.fromIterable(Lists.partition(ids, maxIdsPerQuery))
            .flatMapSequential(subList -> requestObjects(type, subList)
                .doOnSubscribe(ignore -> log.info("Querying {}: {}", type, StringUtils.collectionToCommaDelimitedString(subList))));
    }

    private Flux<Map<String, Object>> queryPlaylists(List<String> ids, BatchLoaderEnvironment env) {
        return Flux.fromIterable(ids)
            .flatMapSequential(id -> requestObject(uriBuilder -> uriBuilder
                .pathSegment("playlists", id)
                .queryParam("additional_types", "track,episode")
                .build())
                .doOnSubscribe(ignore -> log.info("Querying playlist: {}", id)));
    }

    private Flux<Map<String, Object>> queryUsers(List<String> ids, BatchLoaderEnvironment env) {
        return Flux.fromIterable(ids)
            .flatMapSequential(id -> requestObject(uriBuilder -> uriBuilder
                .pathSegment("users", id)
                .build())
                .doOnSubscribe(ignore -> log.info("Querying user: {}", id)));
    }

    private Flux<Map<String, Object>> queryUrls(List<String> urls) {
        return Flux.fromIterable(urls)
            .flatMapSequential(url -> requestObject(uriBuilder -> URI.create(uriBuilder.build() + url))
                .doOnSubscribe(ignore -> log.info("Query raw url: {}", url)));
    }

    private Flux<Try<Map<String, Object>>> requestObjects(String type, List<String> ids) {
        return getSpotifyAuthorization()
            .flatMap(spotifyAuthorization -> webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .pathSegment(type)
                    .queryParam("ids", String.join(",", ids))
                    .build())
                .header(HttpHeaders.AUTHORIZATION, spotifyAuthorization)
                .retrieve()
                .bodyToMono(OBJECTS_RESPONSE_TYPE)
            ).flatMapMany(response -> mapResponse(response, type, ids));
    }

    private Flux<Try<Map<String, Object>>> mapResponse(Map<String, List<Map<String, Object>>> response, String type, List<String> ids) {
        var objects = response.get(type);
        if (objects.size() != ids.size()) {
            return Flux.error(() -> new RuntimeException(String.format("The %s response contains the wrong number of objects. expected %s but got %s", type, ids.size(), objects.size())));
        }

        var result = new ArrayList<Try<Map<String, Object>>>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            var object = objects.get(i);
            if (object == null) {
                result.add(Try.failed(new ObjectNotFoundException(String.format("%s object with id '%s' not found", type, ids.get(i)))));
            } else {
                result.add(Try.succeeded(object));
            }
        }
        return Flux.fromIterable(result);
    }

    private Mono<Map<String, Object>> requestObject(Function<UriBuilder, URI> uriFunction) {
        return getSpotifyAuthorization()
            .flatMap(spotifyAuthorization -> webClient
                .get()
                .uri(uriFunction)
                .header(HttpHeaders.AUTHORIZATION, spotifyAuthorization)
                .retrieve()
                .bodyToMono(OBJECT_RESPONSE_TYPE)
            );
    }

    private Mono<String> getSpotifyAuthorization() {
        return Mono.deferContextual(Mono::just)
            .flatMap(context -> Mono.justOrEmpty(context.<String>getOrEmpty(AuthenticationGraphQlInterceptor.SPOTIFY_AUTHORIZATION_CONTEXT_KEY)))
            .switchIfEmpty(Mono.error(new MissingAuthorizationException("Missing authorization for spotify")));
    }
}
