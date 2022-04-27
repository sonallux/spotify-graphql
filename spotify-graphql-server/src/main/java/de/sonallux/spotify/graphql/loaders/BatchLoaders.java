package de.sonallux.spotify.graphql.loaders;

import com.google.common.collect.Lists;
import de.sonallux.spotify.graphql.AuthenticationGraphQlHandlerInterceptor;
import de.sonallux.spotify.graphql.exception.MissingAuthorizationException;
import lombok.extern.slf4j.Slf4j;
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

        batchLoaderRegistry.<String, Map<String, Object>>forName("albumLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("albums", 20, ids));
        batchLoaderRegistry.<String, Map<String, Object>>forName("artistLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("artists", 50, ids));
        batchLoaderRegistry.<String, Map<String, Object>>forName("episodeLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("episodes", 50, ids));
        batchLoaderRegistry.<String, Map<String, Object>>forName("playlistLoader").registerBatchLoader((ids, env) ->
            queryPlaylists(ids));
        batchLoaderRegistry.<String, Map<String, Object>>forName("showLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("shows", 50, ids));
        batchLoaderRegistry.<String, Map<String, Object>>forName("trackLoader").registerBatchLoader((ids, env) ->
            queryBaseObjects("tracks", 50, ids));

        batchLoaderRegistry.<String, Map<String, Object>>forName("rawLoader").registerBatchLoader((urls, env) ->
            queryUrls(urls));
    }

    private Flux<Map<String, Object>> queryBaseObjects(String type, int maxIdsPerQuery, List<String> ids) {
        return Flux.fromIterable(Lists.partition(ids, maxIdsPerQuery))
            .flatMapSequential(subList -> requestObjects(type, subList)
                .doOnSubscribe(ignore -> log.info("Querying {}: {}", type, StringUtils.collectionToCommaDelimitedString(subList))));
    }

    private Flux<Map<String, Object>> queryPlaylists(List<String> ids) {
        return Flux.fromIterable(ids)
            .flatMapSequential(id -> requestObject(uriBuilder -> uriBuilder
                .pathSegment("playlists", id)
                .queryParam("additional_types", "track,episode")
                .build())
                .doOnSubscribe(ignore -> log.info("Querying playlist: {}", id)));
    }

    private Flux<Map<String, Object>> queryUrls(List<String> urls) {
        return Flux.fromIterable(urls)
            .flatMapSequential(url -> requestObject(uriBuilder -> URI.create(uriBuilder.build() + url))
                .doOnSubscribe(ignore -> log.info("Query raw url: {}", url)));
    }

    private Flux<Map<String, Object>> requestObjects(String type, List<String> ids) {
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
            ).flatMapIterable(object -> object.get(type));
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
            .flatMap(context -> Mono.justOrEmpty(context.<String>getOrEmpty(AuthenticationGraphQlHandlerInterceptor.SPOTIFY_AUTHORIZATION_CONTEXT_KEY)))
            .switchIfEmpty(Mono.error(new MissingAuthorizationException("Missing authorization for spotify")));
    }
}
