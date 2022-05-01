package de.sonallux.spotify.graphql.security;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@RequiredArgsConstructor
public class AuthenticationGraphQlInterceptor implements WebGraphQlInterceptor {
    public static final String SPOTIFY_AUTHORIZATION_CONTEXT_KEY = "spotify_authorization";

    private final AuthorizationService authorizationService;

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return getSpotifyAuthorizationFromOauth2()
            .switchIfEmpty(getSpotifyAuthorizationFromRequest(request))
            .map(this::getSpotifyAuthorizationContext)
            .defaultIfEmpty(Context.empty())
            .flatMap(context -> chain.next(request).contextWrite(context));
    }

    private Mono<String> getSpotifyAuthorizationFromRequest(WebGraphQlRequest request) {
        return Mono.justOrEmpty(request.getHeaders().getFirst(AUTHORIZATION));
    }

    private Mono<String> getSpotifyAuthorizationFromOauth2() {
        return authorizationService.getCurrentOAuth2AuthorizedClient()
            .map(authorizedClient -> {
                var token = authorizedClient.getAccessToken();
                return token.getTokenType().getValue() + " " + token.getTokenValue();
            });
    }

    private Context getSpotifyAuthorizationContext(String spotifyAuthorization) {
        return Context.of(SPOTIFY_AUTHORIZATION_CONTEXT_KEY, spotifyAuthorization);
    }
}
